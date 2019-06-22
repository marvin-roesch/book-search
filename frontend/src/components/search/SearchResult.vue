<template>
<div :class="{ 'search-result': true, 'search-result-with-siblings': showSiblings }"
     @click="toggleSiblings">
  <h2 v-if="displayMetadata">{{ result.book.title }} - {{ result.chapter.title }}</h2>
  <BidirectionalExpandable class="book-text" :expanded="showSiblings" :visible-height="24">
    <template slot="start">
    <p :class="paragraph.classes" v-html="paragraph.text"
       v-for="paragraph in result.prevParagraphs" :key="paragraph.position">
    </p>
    </template>
    <p :class="result.mainParagraph.classes" v-html="result.mainParagraph.text"></p>
    <template slot="end">
    <p :class="paragraph.classes" v-html="paragraph.text"
       v-for="paragraph in result.nextParagraphs" :key="paragraph.position">
    </p>
    <a href="#" @click.prevent.stop="showContent = true" v-if="displayMetadata">
      Read Chapter
    </a>
    </template>
  </BidirectionalExpandable>
  <transition
    name="fade-slide-up"
    @after-enter="addBodyClass"
    @leave="removeBodyClass">
    <ChapterOverlay
      :id="result.chapter.id"
      :query="query"
      :book-title="result.book.title"
      :title="result.chapter.title"
      :position="result.mainParagraph.position"
      @close="showContent = false"
      v-if="showContent">
    </ChapterOverlay>
  </transition>
</div>
</template>

<script>
import BidirectionalExpandable from '@/components/BidirectionalExpandable.vue';
import ChapterOverlay from '@/components/search/ChapterOverlay.vue';

export default {
  name: 'search-result',
  components: {
    ChapterOverlay,
    BidirectionalExpandable,
  },
  props: {
    query: String,
    result: Object,
    displayMetadata: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      showSiblings: false,
      showContent: false,
    };
  },
  methods: {
    toggleSiblings() {
      if (window.getSelection().type !== 'Range') {
        this.showSiblings = !this.showSiblings;
      }
    },
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
.search-result {
  box-sizing: border-box;
  background: white;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
  padding: 1rem;
  margin: 1rem 0;
  position: relative;
  cursor: pointer;

  h2 {
    margin: 0;
  }

  & > .book-text {
    .chapterText {
      text-indent: 0;
    }
  }
}
</style>
