java-reference

- Hard reference
- Soft reference
- Weak reference
- Phantom reference

Hard reference
- 기본 참조 유형
```java
// Hard reference
// Hard reference 이기 때문에 참조가 있는 경우 GC 대상이되지 않는다.
ClassA a = new ClassA(); 

a = null; // GC 대상
```

Soft reference
- 기본적으로 GC의 대상이 된다.
  - 해당 객체에 대한 참조가 Soft reference 뿐인 경우)
- GC는 heap 메모리가 부족한 경우 메모리에서 제거한다.
- 모든 softReference 는 OOM 발생 전 메모리에서 제거된다.
```java
SoftReference<ClassA> softA = new SoftReference<ClassA>(new ClassA());
ClassA a = a.get;

if (a == null) {
	// heap 메모리 부족으로 메모리에서 제거된 상태
} else {
	// GC 대상이지만 heap 메모리가 공간이 있으므로 제거되지 않은 상태
}
```

Weak reference
- 매 GC마다 대상이 되어 제거된다.
- Soft reference 와 다른 점은 heap 메모리 여유와 관계없이 해당 객체에 대한 참조가 Weak reference 뿐이라면 제거한다는 점이다.
- 대표 사용예 (`WeakHashMap`)


---

https://www.baeldung.com/java-reference-types