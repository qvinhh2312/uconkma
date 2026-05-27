/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{js,jsx}"],
  theme: {
    extend: {
      colors: {
        ink: "#17211d",
        moss: "#566b45",
        clay: "#b8613f",
        sand: "#f2eadc",
        paper: "#fffaf0",
      },
      fontFamily: {
        display: ["Georgia", "Cambria", "serif"],
        body: ["Aptos", "Segoe UI", "sans-serif"],
      },
      boxShadow: {
        soft: "0 18px 45px rgba(61, 45, 31, 0.12)",
      },
    },
  },
  plugins: [],
};
