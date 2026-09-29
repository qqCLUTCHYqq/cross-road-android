// Android-only lifecycle. Reuses the bundle's PCM mixer unchanged; no iOS route recreation.
export class AudioLifecycle {
  constructor(owner) { this.owner=owner; this.hidden=false; this.active=false; this.epoch=0; this.serial=0; }
  invalidate() {
    this.active=false; this.serial++;
    this.owner.queueReporter.stop();
    this.owner.queueReporter.epoch=++this.epoch;
    this.owner.graph.resetPcm();
  }
  open(gesture=false) {
    if(this.hidden)return Promise.resolve(false);
    const graph=this.owner.graph, ctx=graph.open(), serial=this.serial;
    if(this.watched!==ctx) {
      this.watched=ctx;
      ctx.onstatechange=()=>{
        graph.onStateChange();
        if(ctx!==graph.ctx)return;
        if(ctx.state==='running'&&!this.hidden){this.active=true;this.owner.queueReporter.start();}
        else if(this.active)this.invalidate();
      };
    }
    const ready=()=>{
      if(serial!==this.serial||this.hidden||ctx!==graph.ctx)return false;
      this.active=ctx.state==='running';
      if(this.active)this.owner.queueReporter.start();
      return this.active;
    };
    if(ctx.state==='running')return Promise.resolve(ready());
    // WebView requires a real tap for the first unlock. Later resume uses same context.
    return ctx.resume().then(ready,()=>false);
  }
  setBackground(hidden) {
    if(this.hidden===hidden)return;
    this.hidden=hidden;this.invalidate();
    if(hidden)this.owner.graph.ctx?.suspend().catch(()=>{});
    else this.open();
  }
  reset() { this.invalidate(); }
}
