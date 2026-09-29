const log=message=>{document.querySelector('#log').textContent+='\n'+message;console.log(message);};
const worker=new Worker('./runtime-worker.js');
worker.onmessage=({data})=>{
  if(data.type==='probeDone'){log(`PASS: ${data.constructors} native constructors; JNI=${data.jniVersion}`);worker.terminate();}
  else if(data.type==='log')log(data.line);
  else if(data.type==='error')log('FAIL: '+data.error);
};
worker.onerror=event=>log('FAIL: '+event.message);
const canvas=document.createElement('canvas');canvas.width=320;canvas.height=180;document.body.append(canvas);
const offscreen=canvas.transferControlToOffscreen();
worker.postMessage({type:'bootProbe',canvas:offscreen},[offscreen]);
