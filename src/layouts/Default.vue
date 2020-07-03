<template>
  <div id="app">
    <top-bar class="top-bar"></top-bar>

    <main class="main">
      <slot />
    </main>
  </div>
</template>

<script>
import TopBar from "~/components/TopBar";

export default {
  components: {
    TopBar,
  },
};
</script>

<style lang="scss" scoped>
.top-bar {
  margin-top: -80px;
}
.main {
  //   margin: 0 auto;
  //   padding: 1.5vw 15px 0;
  margin: 80px;
}
</style>
