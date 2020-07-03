<template>
  <div class="dropdown">
    <span>{{ menuName }}</span>
    <div class="dropdown-content" v-if="edges">
      <g-link
        :class="
          `dropdown-item ${
            menuName === edge.node.title ? 'current-page' : 'not-current-page'
          }`
        "
        v-for="edge in edges"
        :key="edge.node.id"
        :to="edge.node.path"
      >
        {{ edge.node.title }}
      </g-link>
    </div>
  </div>
</template>

<script>
export default {
  props: ["menuName", "edges"],
};
</script>

<style scoped></style>
