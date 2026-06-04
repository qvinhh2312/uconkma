# Mobile App Clean Architecture Roadmap

Thu muc `mobile/` la app Expo/React Native de demo UCONKMA tren Android.

## 1. Pham vi

App mobile tap trung vao:

- login role `ADMIN` / `STUDENT`
- student xem ho so va diem cua minh
- admin xem danh sach sinh vien
- gui `REGISTER` / `DROP`
- xem `DecisionTrace`
- demo monitoring / revoke
- xem policy catalog, PAP summary va validation evidence

App khong mo rong thanh LMS/SIS day du, khong JWT production, khong microservice.

## 2. Clean Architecture

```text
mobile/src
├── app
│   ├── di.ts
│   ├── App.tsx
│   ├── navigation
│   └── providers
├── core
│   ├── config
│   ├── errors
│   ├── network
│   ├── storage
│   └── theme
├── domain
│   ├── entities
│   ├── repositories
│   └── usecases
├── data
│   ├── datasources
│   └── repositories
├── presentation
│   ├── components
│   ├── hooks
│   └── screens
└── shared
    └── data
```

Dependency rule:

- `presentation` goi `usecases`
- `usecases` goi repository interface
- `data/repositories` implement repository interface
- `data/datasources` moi duoc dung Axios/backend API
- `domain` khong import React, Axios, Expo hay storage

## 3. Endpoint dang dung

```text
POST /api/auth/login
POST /api/auth/logout
GET  /api/students/me
GET  /api/students/me/grades
GET  /api/students
GET  /api/classes
POST /api/register
POST /api/drop
POST /api/demo/monitor/maintenance
POST /api/demo/monitor/class-status
POST /api/demo/monitor/student-hold
POST /api/demo/monitor/recheck
GET  /api/pap/policies
GET  /api/pap/summary
POST /api/pap/transition
POST /api/pap/reload
```

## 4. Run Android Emulator

Backend:

```powershell
cd E:\UCON_KMA\engine
mvn spring-boot:run
```

Mobile:

```powershell
cd E:\UCON_KMA\mobile
npm install
npm run android
```

Default backend URL cho Android Emulator:

```text
http://10.0.2.2:8080/api
```

Neu dung dien thoai that, sua `mobile/app.json`:

```json
{
  "expo": {
    "extra": {
      "apiBaseUrl": "http://<LAN-IP>:8080/api"
    }
  }
}
```

## 5. Completion checklist

- [x] Expo TypeScript skeleton
- [x] Clean Architecture folder boundary
- [x] DI container
- [x] Axios client + bearer token
- [x] SecureStore session
- [x] Login screen
- [x] Dashboard
- [x] Student portal
- [x] Admin students
- [x] Register / Drop simulator
- [x] Decision trace viewer
- [x] Monitoring demo
- [x] Policy explorer
- [x] PAP lifecycle screen
- [x] Validation report screen
- [x] TypeScript typecheck pass
- [ ] Android emulator visual smoke test
- [ ] End-to-end demo screenshots
- [ ] Optional: replace bottom tab overflow with drawer navigation
- [ ] Optional: add form validation library
- [ ] Optional: add query cache such as TanStack Query
- [ ] Optional: add EAS Build profile for APK/AAB

## 6. Demo script

1. Login `sv001/student123`.
2. Vao `Simulator`, khong bat `confirmedRegistrationRule`, submit `REGISTER`.
3. Ky vong `DENY`, failed policy `P17_AgreeRegistrationRule_PreB0`.
4. Bat `confirmedRegistrationRule`, submit lai voi class hop le.
5. Vao `Trace`, chi ra `PRE / ONGOING / POST`.
6. Vao `Monitor`, bat maintenance va doc `checkedSessions/revokedSessions`.
7. Vao `Policies`, filter/search ongoing policies.
8. Logout, login `admin/admin123`.
9. Vao `Students`, chung minh admin xem duoc danh sach sinh vien.

## 7. Luu y

- `npm audit` co the bao moderate advisory trong Expo CLI/config dependency tree. Day la dependency build-tool cua Expo SDK, khong nam trong business code UCON.
- Truoc khi release, chay `npx expo install --check`, `npm run typecheck`, sau do build bang EAS hoac Android Studio.
