<template>
  <div class="post__content" v-html="content" />
</template>

<script>
export default {
  props: ["content"],
};
</script>

<style></style>
