# UCONKMA Mobile Demo

Expo Android app for demonstrating the UCONKMA policy engine.

## Architecture

The codebase follows Clean Architecture boundaries:

- `domain`: entities, repository contracts, use cases.
- `data`: API data source and repository implementations.
- `core`: network, storage, config, theme, error normalization.
- `presentation`: screens, reusable UI components and hooks.
- `app`: dependency injection, navigation and providers.

## Run

Backend:

```powershell
cd E:\UCON_KMA\engine
mvn spring-boot:run
```

Android app:

```powershell
cd E:\UCON_KMA\mobile
npm install
npm run android
```

The default API base URL is `http://10.0.2.2:8080/api`, which is correct for the Android Emulator.
For a physical Android device, change `extra.apiBaseUrl` in `app.json` to your machine LAN IP, for example `http://192.168.1.10:8080/api`.

## Demo Accounts

- Admin: `admin` / `admin123`
- Students: `sv001` ... `sv010` / `student123`
