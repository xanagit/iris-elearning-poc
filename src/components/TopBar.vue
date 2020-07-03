<template>
  <div class="shadow">
    <div class="top-bar ">
      <div></div>
      <div>
        <b class="navigate" v-if="currIdx > 0">
          <g-link :to="prevPage" class="arrow">⇜</g-link>
        </b>
        <dropdown :menu-name="currentPageName" :edges="edges"></dropdown>
        <b class="navigate" v-if="currIdx < $page.posts.edges.length">
          <g-link :to="nextPage" class="arrow">⇝</g-link>
        </b>
        <search></search>
      </div>
      <div></div>
    </div>
    <div class="top-bar-height"></div>
  </div>
</template>

<script>
import Dropdown from "~/components/Dropdown";
import Search from "~/components/Search.vue";

export default {
  name: "TopBar",
  components: { Dropdown, Search },
  data: () => ({
    edges: [],
    pages: undefined,
    currentPageName: "",
    prevPage: undefined,
    nextPage: undefined,
    currIdx: 0,
  }),
  created() {
    this.initNavigation();
  },
  watch: {
    $route(to, from) {
      this.initNavigation();
    },
  },
  methods: {
    initNavigation: function() {
      const edges = [
        { node: { id: "id-accueil", path: "/", title: "Accueil" } },
        ...this.$page.posts.edges,
      ];
      this.edges = edges;

      const currEdge = edges.find(
        (el) => el.node.path == `/${this.$route.params.title}/`
      );
      this.currentPageName = currEdge ? currEdge.node.title : "Accueil";
      this.currIdx = edges.findIndex(
        (el) => el.node.path == `/${this.$route.params.title}/`
      );
      this.currIdx = this.currIdx === -1 ? 0 : this.currIdx;

      this.prevPage =
        edges[this.currIdx - 1] !== undefined
          ? edges[this.currIdx - 1].node.path
          : edges[0].node.path;
      this.nextPage =
        edges[this.currIdx + 1] !== undefined
          ? edges[this.currIdx + 1].node.path
          : edges[edges.length - 1].node.path;
    },
  },
};
</script>

<static-query>
query {
  posts: allPost(filter: { published: { eq: true }}) {
    edges {
      node {
        id
        title
        date (format: "D. MMMM YYYY")
        timeToRead
        description
        cover_image (width: 770, height: 380, blur: 10)
        path
        tags {
          id
          title
          path
        }
      }
    }
  }
}
</static-query>

<style lang="scss" scoped>
.top-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  position: fixed;
  width: 100%;
  background-color: #fcfcfc;
  z-index: 99;
  padding: 10px;
  margin-top: 0px;
}

.shadow {
  -webkit-box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
  -moz-box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
  box-shadow: 0 0 10px rgba(0, 0, 0, 0.5);
}

a:link,
a:visited,
a:hover,
a:active {
  text-decoration: none;
  color: #757373;
}

a:hover {
  color: #3b3b3b !important;
  font-weight: bold;
}
.navigate {
  padding-left: 30px;
  padding-right: 30px;
}

.top-bar-height {
  height: 50px;
}

.arrow {
  font-size: 1.5em;
}
</style>
