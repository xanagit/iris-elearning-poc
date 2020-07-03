// Import main css
import "~/assets/style/index.scss";
import "~/assets/style/main.css";

// Import default layout so we don't need to import it to every page
import DefaultLayout from "~/layouts/Default.vue";
import "prismjs";
import "~/assets/style/prism-vs.css";
import "prismjs/plugins/autolinker/prism-autolinker.min";
import Prism from "vue-prism-component";

import "prismjs/components/prism-typescript";
import "prismjs/components/prism-java";
import "prismjs/components/prism-markdown";

// import "~/plugins/vuetify";

// The Client API can be used here. Learn more: gridsome.org/docs/client-api
export default function(Vue, { appOptions, head }) {
  head.link.push({
    rel: "stylesheet",
    href:
      "https://cdn.jsdelivr.net/npm/@mdi/font@latest/css/materialdesignicons.min.css",
  });

  head.link.push({
    rel: "stylesheet",
    href:
      "https://fonts.googleapis.com/css?family=Roboto:100,300,400,500,700,900",
  });

  Vue.component("prism", Prism);
  // Set default layout as a global component
  Vue.component("Layout", DefaultLayout);
}
