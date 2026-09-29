const log=message=>{document.querySelector('#log').textContent+='\n'+message;console.log(message);};
// Separate diagnostic DB; never insert synthetic values into a real game save.
const storage=indexedDB.open('crossroad-android-storage-probe',1);
storage.onupgradeneeded=()=>storage.result.createObjectStore('probe');
storage.onsuccess=()=>{
  const db=storage.result,tx=db.transaction('probe','readwrite'),store=tx.objectStore('probe');
  const read=store.get('persisted');
  read.onsuccess=()=>{
    const prior=read.result===true;store.put(true,'persisted');
    tx.oncomplete=()=>{log(prior?'PASS: IndexedDB persisted across process restart':'PASS: IndexedDB probe write committed');db.close();};
  };
  tx.onerror=()=>log('FAIL: IndexedDB transaction');
};
storage.onerror=()=>log('FAIL: IndexedDB open');
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
