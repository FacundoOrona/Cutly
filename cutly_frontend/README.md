# Cutly Frontend

## Configuración local

Copiá `.env.example` a `.env.local` y configurá `VITE_API_BASE_URL` con la URL base del backend, sin agregar la ruta `/auth`. Por ejemplo, si Spring Boot escucha en `http://localhost:8080`, usá:

```env
VITE_API_BASE_URL=http://localhost:8080
```

Reiniciá Vite después de cambiar variables `VITE_*`. El backend debe permitir las solicitudes del origen local de Vite mediante su configuración CORS.

La sesión se conserva en `sessionStorage` durante la pestaña del navegador. El token sigue siendo accesible para JavaScript y, por lo tanto, ante una vulnerabilidad XSS; no equivale a una cookie HttpOnly. La respuesta del backend incluye `fechaExpiracion`, pero el frontend no la interpreta hasta confirmar su formato y semántica.

## Desarrollo

```sh
npm run dev
```

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Oxc](https://oxc.rs)
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/)

## React Compiler

The React Compiler is not enabled on this template because of its impact on dev & build performances. To add it, see [this documentation](https://react.dev/learn/react-compiler/installation).

## Expanding the ESLint configuration

If you are developing a production application, we recommend using TypeScript with type-aware lint rules enabled. Check out the [TS template](https://github.com/vitejs/vite/tree/main/packages/create-vite/template-react-ts) for information on how to integrate TypeScript and [`typescript-eslint`](https://typescript-eslint.io) in your project.
