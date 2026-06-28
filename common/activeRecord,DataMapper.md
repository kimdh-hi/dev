# Active Record Pattern, Data Mapper ORM

## Active Record Pattern

- 데이터베이스 테이블은 클래스로 매핑
- 레코드는 인스턴스에 해당
- 데이터 접근 로직 또한 도메인 객체 안에 두는 방식
- Ruby on Rails 에서 가장 흔하게 사용
- node 계열의 Sequelize, TypeORM 등을 통해 Active Record Pattern 지원 라이브러리
    - TypeORM 은 Data Mapper ORM 방식도 지원한다.

```jsx
@Entity()
export class User extends BaseEntity {
    @PrimaryGeneratedColumn()
    id: number

    @Column()
    firstName: string

    @Column()
    isActive: boolean

    static findByName(firstName: string, lastName: string) {
        return this.createQueryBuilder("user")
            .where("user.firstName = :firstName", { firstName })
            .getMany()
    }
}

const user = new User()
user.firstName = "Alice"
await user.save()

const users = await User.findBy({ isActive: true })
await user.remove()
```

==> 클래스 내에 모든 데이터 접근 로직이 모이르모 거대한 클래스가 탄생됨 (Fat Model)
==> 단순한 애플리케이션의 경우 적용해도 된다고 하지만 굳이 선택할 이유 모르겠음.

## Data Mapper ORM

- 테이블에 매핑되는 순수 데이터 구조만을 클래스(model)에 정의
- 모든 쿼리 메서드는 별도 클래스로 분리 (repository)
    - 엔티티(model)에는 DB 접근 로직은 없고, 도메인 로직(비즈니스 규칙)은 포함
- spring/jpa 가 대표적