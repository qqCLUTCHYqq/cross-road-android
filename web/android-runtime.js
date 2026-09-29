// Standard Chromium Pointer Events only. Native worker's normalized input ABI is retained.
export function gamePoint(canvas,event) {
  const r=canvas.getBoundingClientRect();
  if(!r.width||!r.height)return null;
  const scale=Math.min(r.width/canvas.width,r.height/canvas.height);
  const width=canvas.width*scale,height=canvas.height*scale;
  const left=r.left+(r.width-width)/2,top=r.top+(r.height-height)/2;
  return {x:Math.max(0,Math.min(1,(event.clientX-left)/width)),y:Math.max(0,Math.min(1,(event.clientY-top)/height)),inside:event.clientX>=left&&event.clientX<=left+width&&event.clientY>=top&&event.clientY<=top+height};
}
export class MobilePointer {
  constructor(canvas,post,unlock) { this.canvas=canvas;this.post=post;this.unlock=unlock;this.active=null;this.last=null;this.handlers=[];this.sequence=0; }
  listen(target,name,handler,options={passive:false}) {target.addEventListener(name,handler,options);this.handlers.push(()=>target.removeEventListener(name,handler,options));}
  emit(kind,event) {
    const point=event?gamePoint(this.canvas,event):this.last;if(!point)return;
    this.last=point;this.post({type:'input',kind,x:point.x,y:point.y,inputSequence:this.sequence,inputTrace:kind!==2});
  }
  finish(event,kind=3) {
    if(this.active===null||(event&&event.pointerId!==this.active))return;
    const id=this.active;this.active=null;this.emit(kind,event);
    try{if(this.canvas.hasPointerCapture(id))this.canvas.releasePointerCapture(id);}catch{}
  }
  attach() {
    this.canvas.style.touchAction='none';
    this.listen(this.canvas,'pointerdown',event=>{
      if(event.isPrimary===false||event.button!==0||!gamePoint(this.canvas,event)?.inside)return;
      event.preventDefault();this.finish(null,4);this.active=event.pointerId;this.sequence++;
      try{this.unlock();}catch{}
      this.emit(1,event);this.canvas.setPointerCapture(event.pointerId);
    });
    this.listen(this.canvas,'pointermove',event=>{if(event.pointerId===this.active){event.preventDefault();this.emit(2,event);}});
    this.listen(window,'pointerup',event=>this.finish(event),{capture:true,passive:true});
    this.listen(window,'pointercancel',event=>{if(event.pointerId===this.active)this.finish(null,4);},{capture:true,passive:true});
    this.listen(this.canvas,'lostpointercapture',()=>this.finish(null,4));
    this.listen(window,'blur',()=>this.finish(null,4));
    this.listen(document,'visibilitychange',()=>{if(document.hidden)this.finish(null,4);});
    this.listen(document,'crossroad-overlay',()=>this.finish(null,4));
    this.listen(this.canvas,'contextmenu',event=>event.preventDefault());
  }
  destroy(){this.finish(null,4);this.handlers.splice(0).forEach(remove=>remove());}
}
let installed=false;
export function installMobileInterface(audio) {
  if(installed)return;installed=true;
  let nativeHidden=false;
  const update=()=>audio.setBackground(nativeHidden||document.hidden,'Android visibility');
  globalThis.crossroadSetBackground=hidden=>{nativeHidden=hidden;update();};
  document.addEventListener('visibilitychange',update);
  document.addEventListener('pointerdown',()=>{if(!nativeHidden&&!document.hidden)audio.open();},{capture:true,passive:true});
  const resize=()=>{
    const canvas=document.querySelector('#game');if(!canvas)return;
    const width=innerWidth,height=innerHeight,scale=Math.min(width/canvas.width,height/canvas.height);
    canvas.style.setProperty('width',Math.floor(canvas.width*scale)+'px','important');
    canvas.style.setProperty('height',Math.floor(canvas.height*scale)+'px','important');
  };
  new MutationObserver(resize).observe(document.querySelector('#app'),{childList:true,subtree:true});
  window.addEventListener('resize',resize);resize();update();
}
