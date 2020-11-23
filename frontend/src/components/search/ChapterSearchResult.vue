<template>
<div class="chapter-end-result" @click.stop="showContent">
  <h2>{{ result.book.title }} - {{ result.chapter.title }}</h2>
  <Button
    v-if="citation !== null"
    class="chapter-end-result-cite"
    href="#"
    @click="cite"
    @click.native="$event.stopImmediatePropagation()"
    slim
  >
    Cite
  </Button>
</div>
</template>

<script>
import { buildCitation, copyText } from '@/utils';
import Button from '@/components/Button.vue';

export default {
  name: 'chapter-search-result',
  components: { Button },
  props: {
    query: String,
    result: Object,
    displayBook: {
      type: Boolean,
      default: true,
    },
  },
  computed: {
    citation() {
      return buildCitation(this.result.book, this.result.chapter);
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
    cite() {
      copyText(this.citation);
      this.$notifications.success('A citation for this chapter has been copied to your clipboard!');
    },
  },
};
</script>

<style lang="scss">
.chapter-end-result {
  display: flex;
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
    flex: 1;
    margin: 0;
    padding: 0;
    font-size: 1.25rem;
    z-index: 1001;
  }

  &-cite {
    margin-left: auto !important;
    height: auto !important;
    padding: 0.5rem !important;
    font-size: 0.9rem !important;
  }
}
</style>
