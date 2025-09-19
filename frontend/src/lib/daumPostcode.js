let loaded = false;

export function loadDaumPostcode() {
  return new Promise((resolve, reject) => {
    if (loaded) return resolve();
    const s = document.createElement('script');
    s.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
    s.onload = () => { loaded = true; resolve(); };
    s.onerror = reject;
    document.head.appendChild(s);
  });
}

export async function openPostcode(onComplete) {
  await loadDaumPostcode();
  new window.daum.Postcode({ oncomplete: onComplete }).open(); // 팝업 모드
}
