<template>
  <div class="codes-container">
    <div class="code-top">
      <code-block type="initFiles"></code-block>
    </div>
    <div v-if="displayBottom">
      <button class="button" @click="toggleSolution()">
        <span v-if="displaySolution">Masquer la solution</span>
        <span v-else>Afficher la solution</span>
      </button>
    </div>
    <div class="code-bottom" ref="codeBottom" v-if="displayBottom">
      <code-block type="finalFiles"></code-block>
    </div>
  </div>
</template>

<script>
import CodeBlock from "~/components/CodeBlock";

export default {
  components: {
    CodeBlock,
  },
  data: () => ({
    displaySolution: false,
  }),
  computed: {
    displayBottom() {
      return (
        this.$page.post.finalFiles && this.$page.post.finalFiles.length > 0
      );
    },
  },
  methods: {
    toggleSolution() {
      if (this.displaySolution) {
        this.$refs["codeBottom"].style.display = "none";
        this.displaySolution = false;
      } else {
        this.$refs["codeBottom"].style.display = "block";
        this.displaySolution = true;
      }
    },
  },
};
</script>

<style scoped>
.codes-container {
  display: flex;
  flex-direction: column;
  width: 100%;
  position: absolute;
  top: 0;
  bottom: 0;
  right: 0;
}

.code-top,
.code-bottom {
  flex-grow: 1;
  overflow: auto;
  /* for Firefox */
  min-height: 0;
  padding-left: 10px;
  padding-right: 10px;
  height: 50%;
}

.code-bottom {
  display: none;
}
</style>
