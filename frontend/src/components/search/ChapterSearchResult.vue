<template>
<div class="chapter-end-result" @click="showContent = true">
  <h2>{{ result.book.title }} - {{ result.chapter.title }}</h2>
  <transition
    name="fade-slide-up"
    @after-enter="addBodyClass"
    @leave="removeBodyClass">
    <ChapterOverlay
      :id="result.chapter.id"
      :query="query"
      :book-title="result.book.title"
      :title="result.chapter.title"
      @close="showContent = false"
      v-if="showContent">
    </ChapterOverlay>
  </transition>
</div>
</template>

<script>
import ChapterOverlay from '@/components/search/ChapterOverlay.vue';

export default {
  name: 'chapter-search-result',
  components: { ChapterOverlay },
  props: {
    query: String,
    result: Object,
    displayBook: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      showContent: false,
    };
  },
  methods: {
    addBodyClass() {
      document.body.classList.add('chapter-preview');
    },
    removeBodyClass() {
      document.body.classList.remove('chapter-preview');
    },
  },
};
</script>

<style lang="scss">
.chapter-end-result {
  box-sizing: border-box;
  background: white;
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
