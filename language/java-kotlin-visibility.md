kotlin
- public(`default`): 접근 제한 없음
- private: 선언된 파일 또는 선언된 클래스 내에서만 접근 가능
  - outer class 에서 inner class private 접근 불가
- protected: 자식 클래스에서만 접근 가능 (top-level function 등에 사용 불가)
  - 같은 패키지 내 접근 불가
- internal: 같은 모듈에서 접근 가능

Java
- public: 접근 제한 없음
- protected: `같은 패키지` 또는 자식 클래스에서만 접근 가능
- default(`default`): 같은 패키지에서만 접근 가능 (`package-private`)
- private: 선언된 클래스 내에서만 접근 가능
  - outer class 에서 inner class private 접근 가능
