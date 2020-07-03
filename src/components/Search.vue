<template>
  <div class="dropdown">
    <input
      type="text"
      v-model="searchTerm"
      @input="searchResults"
      placeholder="rechercher"
    />
    <div class="dropdown-content" v-if="resPages">
      <div v-for="res in resPages" :key="res.node.id">
        <g-link :to="res.node.path" class="dropdown-item not-current-page">
          {{ res.node.title }}
        </g-link>
      </div>
    </div>
  </div>
</template>

<script>
const FlexSearch = require("flexsearch");

export default {
  data() {
    return {
      index: null,
      searchTerm: undefined,
      result: undefined,
      resPages: undefined,
    };
  },
  beforeMount() {
    this.index = new FlexSearch({
      encode: "balance",
      tokenize: "forward",
      threshold: 0,
      async: false,
      worker: false,
      cache: false,
    });
    this.$static.posts.edges.forEach((el) => {
      this.index.add(el.node.id, el.node.content.replace(/<[^>]*>?/gm, ""));
    });
  },
  methods: {
    searchResults() {
      this.result = this.index.search(this.searchTerm);
      if (!this.result) return;
      this.resPages = this.$static.posts.edges.filter((p) => {
        return this.result.includes(p.node.id);
      });
      if (this.resPages.length === 0) this.resPages = undefined;
    },
  },
};
</script>
<static-query>
query Posts {
  posts: allPost {
    edges {
      node {
        id
        title
        date
        path
        content
      }
    }
  }
}
</static-query>
<style scoped>
.dropdown {
  width: 20%;
}

.dropdown input {
  outline: none;
  height: 35px;
  font-size: 1.2em;
}

.dropdown input:focus {
  border: 2px solid #008cba;
}
</style>
