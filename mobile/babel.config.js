module.exports = function (api) {
  api.cache(true);
  return {
    presets: ["babel-preset-expo"],
    plugins: [
      [
        "module-resolver",
        {
          root: ["./src"],
          alias: {
            "@app": "./src/app",
            "@core": "./src/core",
            "@data": "./src/data",
            "@domain": "./src/domain",
            "@presentation": "./src/presentation",
            "@shared": "./src/shared"
          }
        }
      ]
    ]
  };
};
