# CASA(Cloud Application Security Assessment)

## CASA(Cloud Application Security Assessment)

- App Defense Alliance 의 클라우드 앱 심사
    - App Defense Alliance 은 google 이 이끌던 조직
    - CASA 는 App Defense Alliance (구글 주도 컨소시엄) 에서 만든 클라우드 앱 보안 심사
- google 은 자사 OAuth 검증 절차에서 restricted scope (제한된 범위) 가 대상인 경우 앱의 보안 평가 표준으로 CASA를 지정
- CASA 는 실제 평가를 실시하는 조직은 아니고 단순히 표준 평가 기준을 정의
    - google 이 돈 버는건 아님..
- CASA 평가 기준대로 승인된 제3자가 앱을 테스트
    - TAC Security, Leviathan, NCC Group, Bishop Fox, NetSentries, KPMG...
- google 의 gmail/drive 등 여러 api 중 restricted scope 에 해당하는 것을 사용하려면 CASA 검증 필요
    - 단, CASA tier 1,2,3 중 어떤 것을 요구하는지 달라짐
    - tier 1: 무료(자가평가) - 사실상 restricted scope 는 전부 tier2,3
    - tier 2: 랩 검증 DAST 스캔, 대략 수백~수천 달러(예: $540~$1,500, 랩·긴급도에 따라 $3,000~$6,000)
    - tier 3: 랩이 직접 하는 풀 침투 테스트, $5,000 이상

==> 사실상 restricted scope 사용시 최소 tier2 이상 검증이 필요
==> mail 조회, drive 조회 등 조회성 api 는 보통 restricted scope

```
용어변경

tier1, tier2, tier3 --> AL0/AL1/AL2 (Assuarance Level)
```

## Reference

- CASA(Cloud Application Security Assessment)
    - https://appdefensealliance.dev/casa?hl=ko
- CASA 보증수준
    - https://appdefensealliance.dev/casa/casa-tiering?hl=ko
- google CASA 관련
    - https://developers.google.com/identity/protocols/oauth2/production-readiness/restricted-scope-verification?hl=ko
- google Restricted Scopes
    - https://support.google.com/cloud/answer/13464325#zippy=
- https://developer.nylas.com/docs/provider-guides/google/google-verification-security-assessment-guide/