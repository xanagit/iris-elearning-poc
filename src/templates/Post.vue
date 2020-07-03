<template>
  <Layout>
    <div class="page-container">
      <div
        class="page-explanation"
        :class="isCodeContent ? 'half-width' : 'full-width'"
      >
        <div class="explanation-container">
          <md-content :content="$page.post.content"></md-content>
        </div>
      </div>
      <div class="page-code" v-if="isCodeContent">
        <div class="code-container">
          <code-content></code-content>
        </div>
      </div>
    </div>
  </Layout>
</template>

<script>
import MdContent from "~/components/MdContent";
import CodeContent from "~/components/CodeContent";

export default {
  components: {
    MdContent,
    CodeContent,
  },
  data: () => ({
    isCodeContent: false,
  }),
  created() {
    this.checkIfCodeContent();
  },
  watch: {
    $route(to, from) {
      this.checkIfCodeContent();
    },
  },
  methods: {
    checkIfCodeContent() {
      const initFilles = this.$page.post.initFiles;
      const finalFiles = this.$page.post.finalFiles;
      const isInitFile = initFilles !== undefined && initFilles.length > 0;
      const isFinalFile = finalFiles !== undefined && finalFiles.length > 0;

      this.isCodeContent = isInitFile || isFinalFile;
    },
  },
  metaInfo() {
    return {
      title: this.$page.post.title,
      meta: [
        {
          name: "description",
          content: this.$page.post.description,
        },
      ],
    };
  },
};
</script>

<page-query>
query Post ($id: ID!) {
  post: post (id: $id) {
    title
    path
    excerpt
    date (format: "D. MMMM YYYY")
    timeToRead
    tags {
      id
      title
      path
    }
    description
    initFiles
    finalFiles
    content
    cover_image (width: 860, blur: 10)
  }

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

</page-query>

<style lang="scss" scoped>
.page-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-explanation {
  display: flex;
  flex-direction: column;

  position: absolute;
  top: 80px;
  bottom: 0;
  left: 0;
}

.full-width {
  width: 100%;
}

.half-width {
  width: 40%;
  //   border-right: 5px solid rgb(0, 127, 231);
}

.explanation-container {
  flex-grow: 1;
  overflow: auto;
  min-height: 0;
  padding-left: 10px;
  padding-right: 10px;
}

.page-code {
  display: flex;
  flex-direction: column;
  width: 60%;
  position: absolute;
  top: 80px;
  bottom: 0;
  right: 0;
}

.code-container {
  flex-grow: 1;
  overflow: auto;
  min-height: 0;
  padding-left: 10px;
  padding-right: 10px;
}
</style>
