import Vue from "vue";
import Vuetify from "vuetify";
import "vuetify/dist/vuetify.min.css";

Vue.use(Vuetify, {
  theme: {
    themes: {
      light: {
        primary: "#3f51b5",
        secondary: "#b0bec5",
        accent: "#8c9eff",
        error: "#b71c1c",
      },
    },
  },
});

// export default new Vuetify({
//   theme: {
//     primary: "#00A3F1",
//     secondary: "#454545",
//     accent: "#8c9eff",
//     warning: "#f8a232",
//     error: "#b71c1c",
//     text: "#757575",
//     topBarBackground: "#ffffff",
//     globalBackground: "#54aaff",
//     topBarColor: "#ffffff",
//     topBarColorHover: "#e0e0e0",
//   },
// });

// Vue.use(Vuetify, {
//     theme: {
//         primary: "#00A3F1",
//         secondary: "#454545",
//         accent: "#8c9eff",
//         warning: "#f8a232",
//         error: "#b71c1c",
//         text: "#757575",
//         topBarBackground: "#ffffff",
//         globalBackground: "#54aaff",
//         topBarColor: "#ffffff",
//         topBarColorHover: "#e0e0e0",
//     },
// });

// const opts = {}; //opts includes, vuetify themes, icons, etc.
// appOptions.vuetify = new Vuetify(opts);
