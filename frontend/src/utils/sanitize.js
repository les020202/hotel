// src/utils/sanitize.js
import DOMPurify from 'dompurify'

/**
 * 텍스트만 허용(모든 태그/이벤트 속성 제거) + trim
 * - DOMPurify는 XSS 페이로드를 안전하게 제거
 * - 서버가 최종 방어이지만, 클라이언트 보조 방어로 사용
 */
export function sanitizeText(text = '') {
  const cleaned = DOMPurify.sanitize(String(text), {
    ALLOWED_TAGS: [],
    ALLOWED_ATTR: [],
  })
  // 널문자 제거 + 앞뒤 공백 제거
  return cleaned.replace(/\u0000/g, '').trim()
}

/**
 * 길이 제한 (간단 clamp)
 * - 유니코드 조합문자 완전 분리는 아니지만 일반 텍스트 사용에는 충분
 */
export function clamp(s, maxLen) {
  if (s == null) return s
  const t = String(s)
  return t.length <= maxLen ? t : t.substring(0, maxLen)
}

/**
 * 전송 직전 자주 쓰는 헬퍼: sanitizeText → clamp 순서로 적용
 */
export function sanitizeAndClamp(text, maxLen) {
  return clamp(sanitizeText(text), maxLen)
}

/* ---------------------------------------------
 * 파일 업로드 클라이언트 보조검사 (최종 검증은 반드시 서버에서)
 * --------------------------------------------- */

export const ALLOWED_EXT = ['jpg', 'jpeg', 'png', 'webp', 'gif']
export const MAX_FILE_SIZE = 5 * 1024 * 1024 // 5MB

export const extOf = (n = '') =>
  (n.lastIndexOf('.') < 0 ? '' : n.slice(n.lastIndexOf('.') + 1).toLowerCase())

/**
 * 브라우저에서 1차 필터링(확장자/용량)
 * - 실제 컨텐츠 타입/매직바이트 검사는 서버에서 필수!
 */
export function validateFiles(fileList) {
  const files = Array.from(fileList || [])
  const ok = []
  const errors = []
  for (const f of files) {
    const ext = extOf(f.name)
    if (!ALLOWED_EXT.includes(ext)) {
      errors.push(`허용되지 않는 확장자: ${f.name}`)
      continue
    }
    if (f.size > MAX_FILE_SIZE) {
      errors.push(`용량 초과(5MB): ${f.name}`)
      continue
    }
    ok.push(f)
  }
  return { ok, errors }
}

/* ---------------------------------------------
 * (선택) 텍스트 내 URL을 토큰으로 분리해 v-html 없이 링크 렌더링
 *  - 소비 측 예시:
 *    tokenizeWithLinks(text).map(t => t.type==='link'
 *      ? h('a', { href:t.href, target:'_blank', rel:'noopener' }, t.text)
 *      : t.text)
 * --------------------------------------------- */
const URL_RE = /\b((https?:\/\/)[^\s<>"']+)\b/gi
export function tokenizeWithLinks(text) {
  const t = String(text ?? '')
  const tokens = []
  let last = 0
  let m
  while ((m = URL_RE.exec(t)) !== null) {
    if (m.index > last) tokens.push({ type: 'text', text: t.slice(last, m.index) })
    tokens.push({ type: 'link', text: m[1], href: m[1] })
    last = m.index + m[0].length
  }
  if (last < t.length) tokens.push({ type: 'text', text: t.slice(last) })
  return tokens
}
