// 한 번만 로드해서 재사용
let loading = null;

export function loadDaumPostcode() {
  if (window.daum?.Postcode) return Promise.resolve(window.daum);
  if (loading) return loading;
  loading = new Promise((resolve, reject) => {
    const s = document.createElement('script');
    s.src = '//t1.daumcdn.net/mapjsapi/bundle/postcode/prod/postcode.v2.js';
    s.onload = () => resolve(window.daum);
    s.onerror = reject;
    document.head.appendChild(s);
  });
  return loading;
}
