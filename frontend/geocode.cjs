// geocode.cjs  — CommonJS 버전 (진행 로그 포함)
// 실행:  $env:KAKAO_REST_KEY='YOUR_KEY'; node geocode.cjs

const fs = require('fs/promises');

// Node 18+는 fetch 내장, 그 미만은 node-fetch 동적 로드
async function getFetch() {
  if (typeof fetch === 'function') return fetch;
  const { default: nf } = await import('node-fetch');
  return nf;
}

const KAKAO_REST_KEY = process.env.KAKAO_REST_KEY;
if (!KAKAO_REST_KEY) {
  console.error('❌ KAKAO_REST_KEY 환경변수가 없습니다.');
  process.exit(1);
}

// ===== 입력 데이터 (네가 준 JSON 그대로) =====
const INPUT = [
  {"id":1,"address":"서울특별시 영등포구 국제금융로 10  (여의도동)"},
  {"id":2,"address":"인천광역시 중구 공항로424번길 48-27  (운서동)"},
  {"id":3,"address":"서울특별시 강남구 테헤란로29길 11  (역삼동)"},
  {"id":4,"address":"인천광역시 부평구 광장로 10  (부평동)"},
  {"id":5,"address":"서울특별시 영등포구 선유로 88-6 노바루스관관호텔 (양평동1가)"},
  {"id":6,"address":"서울특별시 동작구 시흥대로 596  (신대방동)"},
  {"id":7,"address":"서울특별시 영등포구 여의대로 8  (여의도동)"},
  {"id":8,"address":"울산광역시 남구 삼산로 246 울산시티호텔 (달동)"},
  {"id":9,"address":"서울특별시 광진구 동일로 214 (군자동)"},
  {"id":10,"address":"서울특별시 중구 퇴계로 334  (광희동2가)"},
  {"id":11,"address":"인천광역시 중구 영종해안남로321번길 208  (운서동)"},
  {"id":12,"address":"서울특별시 강남구 논현로 854  (신사동)"},
  {"id":13,"address":"경상남도 통영시 미수해안로 72  (미수동)"},
  {"id":14,"address":"인천광역시 중구 영종해안남로321번길 190  (운서동)"},
  {"id":15,"address":"부산광역시 해운대구 해운대해변로 310 1층 (중동)"},
  {"id":16,"address":"부산광역시 해운대구 해운대해변로237번길 5  (우동)"},
  {"id":17,"address":"인천광역시 연수구 테크노파크로 200  (송도동)"},
  {"id":18,"address":"경상북도 구미시 3공단1로 296  (임수동)"},
  {"id":19,"address":"서울특별시 강남구 논현로 734  (논현동)"},
  {"id":20,"address":"울산광역시 남구 삼산로 204  (달동)"},
  {"id":21,"address":"울산광역시 남구 삼산로 282  (삼산동)"},
  {"id":22,"address":"전라남도 목포시 평화로 79  (상동)"},
  {"id":23,"address":"강원도 춘천시 하중도길 128 레고랜드 호텔 (중도동)"},
  {"id":24,"address":"부산광역시 해운대구 마린시티1로 51  (우동)"},
  {"id":25,"address":"인천광역시 남동구 석촌로46번길 47  (간석동)"},
  {"id":26,"address":"경상남도 창원시 마산합포구 월영동11길 19 (해운동)"},
  {"id":27,"address":"서울특별시 서초구 신반포로 176  (반포동)"},
  {"id":28,"address":"부산광역시 해운대구 동백로 67  (우동)"},
  {"id":29,"address":"서울특별시 종로구 새문안로 97  (당주동)"},
  {"id":30,"address":"강원도 홍천군 두촌면 광석로 898-87 2/2"},
  {"id":31,"address":"인천광역시 중구 월미로248번길 1  (북성동1가)"},
  {"id":32,"address":"전라남도 장흥군 장평면 제산기동로 326"},
  {"id":33,"address":"전라남도 영암군 삼호읍 대불로 91"},
  {"id":34,"address":"전라북도 군산시 한밭안길 30  (나운동)"},
  {"id":35,"address":"경상남도 통영시 미수해안로 152  (봉평동)"},
  {"id":36,"address":"전라남도 여수시 오동도로 111 소노캄 여수 (수정동)"},
  {"id":37,"address":"울산광역시 동구 방어진순환도로 875 라한호텔 울산 (전하동)"},
  {"id":38,"address":"경상북도 경주시 보문로 484-7  (신평동)"},
  {"id":39,"address":"경기도 광명시 일직로12번길 22  (일직동)"},
  {"id":40,"address":"대전광역시 유성구 엑스포로 1  (도룡동)"},
  {"id":41,"address":"경상북도 경주시 보문로 422  (신평동)"},
  {"id":42,"address":"경상북도 청송군 청송읍 중앙로 315"},
  {"id":43,"address":"경기도 안양시 동안구 흥안대로 513  (관양동)"},
  {"id":44,"address":"대구광역시 수성구 팔현길 212  (만촌동)"},
  {"id":45,"address":"경기도 성남시 수정구 창업로 18 나인트리 프리미어 호텔 서울 판교 (시흥동)"},
  {"id":46,"address":"전라북도 군산시 새만금북로 435 . (오식도동)"},
  {"id":47,"address":"대구광역시 동구 동부로26길 6  (신천동)"},
  {"id":48,"address":"강원도 고성군 토성면 미시령옛길 1153 소노캄 호텔 델피노"},
  {"id":49,"address":"인천광역시 중구 용유서로 262-15  (을왕동)"},
  {"id":50,"address":"경상남도 통영시 통영해안로 407  (정량동)"},
  {"id":51,"address":"경기도 용인시 처인구 백암면 고안로 48"},
  {"id":52,"address":"경상북도 경주시 보문로 338  (신평동)"},
  {"id":53,"address":"부산광역시 부산진구 가야대로 772  (부전동)"},
  {"id":54,"address":"강원도 강릉시 강동면 율곡로 1441"},
  {"id":55,"address":"광주광역시 서구 상무누리로 55  (치평동)"},
  {"id":56,"address":"부산광역시 수영구 민락수변로 29 11층, 12층 (민락동)"},
  {"id":57,"address":"충청남도 당진시 송악읍 한진포구길 30-38"},
  {"id":58,"address":"부산광역시 남구 전포대로 133  (문현동)"},
  {"id":59,"address":"대구광역시 동구 팔공산로185길 11 (용수동)"},
  {"id":60,"address":"대전광역시 유성구 엑스포로123번길 33  (도룡동)"},
  {"id":61,"address":"전라북도 남원시 주천면 원천로 217"},
  {"id":62,"address":"경상남도 통영시 도남로 347 (도남동)"},
  {"id":63,"address":"부산광역시 해운대구 해운대해변로 292  (중동)"},
  {"id":64,"address":"충청북도 청주시 청원구 내수읍 신기초정로 699"},
  {"id":65,"address":"부산광역시 해운대구 달맞이길 30  (중동, 엘시티)"},
  {"id":66,"address":"전라남도 여수시 삼산면 거문도등대길 54"},
  {"id":67,"address":"경기도 포천시 소흘읍 죽엽산로 432-17"},
  {"id":68,"address":"경상남도 거제시 장평3로 80-37  (장평동)"},
  {"id":69,"address":"충청북도 음성군 맹동면 장성로 107"},
  {"id":70,"address":"광주광역시 동구 금남로 226-11"},
  {"id":71,"address":"충청북도 충주시 대소원면 메가폴리스2로 63"},
  {"id":72,"address":"전라북도 군산시 가도안1길 45  (오식도동)"},
  {"id":73,"address":"대전광역시 유성구 온천로 81"},
  {"id":74,"address":"충청북도 충주시 수안보면 탑골1길 36"},
  {"id":75,"address":"경기도 고양시 일산동구 태극로 20"},
  {"id":76,"address":"울산광역시 울주군 상북면 석남로 752-8"},
  {"id":77,"address":"전라북도 전주시 덕진구 정언신로 186  (우아동2가)"},
  {"id":78,"address":"부산광역시 서구 충무대로 16  (암남동)"},
  {"id":79,"address":"광주광역시 동구 지호로164번길 14-10  (지산동)"},
  {"id":80,"address":"대전광역시 서구 한밭대로570번길 29-20  (월평동)"},
  {"id":81,"address":"경상북도 울릉군 울릉읍 도동1길 21-1"},
  {"id":82,"address":"대구광역시 중구 국채보상로 611  (문화동)"},
  {"id":83,"address":"대구광역시 동구 팔공산로 1121  (용수동)"},
  {"id":84,"address":"충청남도 천안시 서북구 성정공원5로 42"},
  {"id":85,"address":"광주광역시 서구 상무연하로 46"}
];

const stripParen = (s) => s.replace(/\(.*?\)/g, '').replace(/\s{2,}/g,' ').trim();
const sleep = (ms) => new Promise(r => setTimeout(r, ms));

async function geocode(fetchFn, addr) {
  const url = `https://dapi.kakao.com/v2/local/search/address.json?analyze_type=similar&size=1&query=${encodeURIComponent(addr)}`;
  const res = await fetchFn(url, { headers: { Authorization: `KakaoAK ${KAKAO_REST_KEY}` }});
  if (!res.ok) throw new Error(`HTTP ${res.status}`);
  const j = await res.json();
  const d = (j.documents || [])[0];
  if (!d) return null;
  return { lat: +d.y, lng: +d.x, resolved: d.road_address?.address_name || d.address?.address_name || '' };
}

(async () => {
  const f = await getFetch();
  console.log(`▶ 총 ${INPUT.length}건 지오코딩 시작…`);

  const results = [];
  let ok = 0, fail = 0;

  for (let i = 0; i < INPUT.length; i++) {
    const row = INPUT[i];
    const raw = String(row.address || '').trim();
    const q = stripParen(raw);

    process.stdout.write(`[${i+1}/${INPUT.length}] ${q} … `);
    try {
      const g = await geocode(f, q);
      if (g) {
        results.push({ id: row.id, input: raw, query: q, ...g, status: 'OK' });
        ok++;
        console.log(`OK (${g.lat}, ${g.lng})`);
      } else {
        results.push({ id: row.id, input: raw, query: q, lat: null, lng: null, resolved: null, status: 'NOT_FOUND' });
        fail++;
        console.log('NOT_FOUND');
      }
    } catch (e) {
      results.push({ id: row.id, input: raw, query: q, lat: null, lng: null, resolved: null, status: 'ERROR:'+e.message });
      fail++;
      console.log(`ERROR: ${e.message}`);
    }
    await sleep(250); // 4 req/sec
  }

  // CSV
  const csvHeader = 'id,input,query,lat,lng,resolved,status\n';
  const csvBody = results.map(r =>
    [r.id, r.input, r.query, r.lat, r.lng, r.resolved, r.status]
      .map(v => (v==null?'':String(v)).replaceAll('"','""'))
      .map(v => `"${v}"`).join(',')
  ).join('\n');
  await fs.writeFile('hotels_geocoded.csv', csvHeader + csvBody, 'utf8');

  // SQL
  const sql = results
    .filter(r => r.id != null && r.lat != null && r.lng != null)
    .map(r => `UPDATE hotels SET latitude=${r.lat}, longitude=${r.lng} WHERE id=${r.id};`)
    .join('\n');
  await fs.writeFile('update_hotels.sql', sql + '\n', 'utf8');

  // 실패 로그
  const fails = results.filter(r => r.lat == null || r.lng == null);
  await fs.writeFile('geocode_failures.json', JSON.stringify(fails, null, 2), 'utf8');

  console.log(`\n✅ 완료: OK=${ok}, FAIL=${fail}`);
  console.log('생성: hotels_geocoded.csv, update_hotels.sql, geocode_failures.json');
})().catch(err => {
  console.error('_FATAL_', err);
  process.exit(1);
});
