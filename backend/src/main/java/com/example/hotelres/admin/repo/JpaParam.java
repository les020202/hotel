// JpaQueryExt.kt 또는 Java 유틸로
package com.example.hotelres.admin.repo;
import jakarta.persistence.Query;

public class JpaParam {
  public static Query setParameterOptional(Query q, String name, Object v){
    if (v!=null) q.setParameter(name, v);
    return q;
  }
}
// 사용: em.createQuery(...).let(q -> JpaParam.setParameterOptional(q,"region",p.get("region")) ... )
