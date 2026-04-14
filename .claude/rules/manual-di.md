---
description: Manual dependency injection pattern — no Hilt, no Koin
paths:
  - "app/src/**/*.kt"
---

- This project uses **manual DI** — do NOT use Hilt (`@HiltViewModel`, `@Inject`, `@Module`) or Koin
- ViewModels use companion `Factory` object implementing `ViewModelProvider.Factory`
- Repositories take API interfaces as constructor params with defaults from `RetrofitClient`
- `RetrofitClient` is a singleton `object` with lazy API instances
- Test injection: pass fakes via constructor parameters
