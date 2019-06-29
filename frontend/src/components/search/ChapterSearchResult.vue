<template>
<div class="chapter-end-result" @click.stop="showContent">
  <h2>{{ result.book.title }} - {{ result.chapter.title }}</h2>
</div>
</template>

<script>
export default {
  name: 'chapter-search-result',
  props: {
    query: String,
    result: Object,
    displayBook: {
      type: Boolean,
      default: true,
    },
  },
  methods: {
    showContent() {
      this.$router.push({
        name: 'search-preview',
        params: { id: this.result.chapter.id },
        query: { ...this.$route.query, q: this.query },
      });
    },
  },
};
</script>

<style lang="scss">
.chapter-end-result {
  box-sizing: border-box;
  background: var(--section-bg);
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
  padding: 1rem;
  margin: 1rem 0 0;
  position: relative;
  cursor: pointer;

  h2 {
    position: relative;
    margin: 0;
    padding: 0;
    font-size: 1.25rem;
    z-index: 1001;
  }
}
</style>
