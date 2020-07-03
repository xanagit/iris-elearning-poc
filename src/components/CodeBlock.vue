<template>
  <div class="code-bock" ref="code">
    <div class="tab">
      <button
        v-for="content in contents"
        :key="content.id"
        @click="setCurrentIdx(content.id)"
        :class="
          `${
            currContent.id == content.id
              ? 'selected-tab-btn'
              : 'not-selected-tab-btn'
          }`
        "
      >
        {{ content.name }}
      </button>
    </div>
    <div class="tabcontent">
      <div class="prism-container">
        <div class="prism-code">
          <prism
            :language="currContent.lang"
            :code="currContent.content"
            ref="prism"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import http from "~/services/http";

export default {
  props: ["type"],
  data: () => ({
    contents: [],
    currIdx: 0,
    files: [],
  }),
  watch: {
    $route(to, from) {
      this.refreshContent();
    },
  },
  async created() {
    this.refreshContent();
  },
  mounted() {
    this.handlePrismSize();
    const resizeObserver = new ResizeObserver((_) => {
      this.handlePrismSize();
    });
    resizeObserver.observe(this.codeElem);
  },
  computed: {
    currContent() {
      return this.contents[this.currIdx]
        ? this.contents[this.currIdx]
        : { lang: undefined, content: undefined };
    },

    codeElem() {
      return this.$refs["code"];
    },

    prismElem() {
      return this.$refs["prism"];
    },
  },
  methods: {
    async refreshContent() {
      this.files = this.$page.post[this.type];
      this.contents = await Promise.all(
        this.files.map(async (file, idx) => {
          const res = await http.get(file);
          const content = await res.text();
          const lang = file
            .split(".")
            .slice(-1)
            .pop();
          const name = file
            .split("/")
            .slice(-1)
            .pop();
          return {
            id: idx,
            content: content,
            name: name,
            lang: lang,
          };
        })
      );
    },
    setCurrentIdx(idx) {
      this.currIdx = idx;
    },
    handlePrismSize() {
      this.prismElem.style.height = `${this.codeElem.offsetHeight - 80}px`;
    },
  },
};
</script>

<style scoped>
.code-bock {
  height: 100%;
}
/* Style the tab */
.tab {
  overflow: hidden;
  border: 1px solid #ccc;
  background-color: #f1f1f1;
}

/* Style the buttons that are used to open the tab content */
.tab button {
  background-color: inherit;
  float: left;
  outline: none;
  cursor: pointer;
  padding: 14px 16px;
  transition: 0.3s;
  border-left: none;
  border-right: none;
  border-bottom: none;
}

/* Change background color of buttons on hover */
.tab button:hover {
  background-color: #ddd;
}

/* Create an active/current tablink class */
.tab button.active {
  background-color: #ccc;
}

.not-selected-tab-btn {
  border-top: 4px solid transparent;
}
.selected-tab-btn {
  border-top: 4px solid rgb(0, 127, 231);
}

/* Style the tab content */
.tabcontent {
  position: relative;
  display: block;
  padding: 6px 12px;
  border: 1px solid #ccc;
  border-top: none;
}

/* .tag:not(body) {
  background-color: transparent;
}
.number {
  background-color: transparent;
  vertical-align: baseline;
  font-size: 1em;
  padding: 0;
  margin: 0;
  min-width: 0;
} */
</style>
