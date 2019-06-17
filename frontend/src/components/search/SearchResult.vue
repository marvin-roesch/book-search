<template>
<div :class="{ 'search-result': true, 'search-result-with-siblings': showSiblings }"
     @click="toggleSiblings">
  <h2 v-if="displayMetadata">{{ result.book.title }} - {{ result.chapter.title }}</h2>
  <BidirectionalExpandable :expanded="showSiblings" :visible-height="24">
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
    <router-link :to="{name: 'chapter', params: { id: result.chapter.id }}" v-if="displayMetadata">
      Read Chapter
    </router-link>
    </template>
  </BidirectionalExpandable>
</div>
</template>

<script>
import BidirectionalExpandable from '@/components/BidirectionalExpandable.vue';

export default {
  name: 'search-result',
  components: {
    BidirectionalExpandable,
  },
  props: {
    result: Object,
    displayMetadata: {
      type: Boolean,
      default: true,
    },
  },
  data() {
    return {
      showSiblings: false,
    };
  },
  methods: {
    toggleSiblings() {
      if (window.getSelection().type !== 'Range') {
        this.showSiblings = !this.showSiblings;
      }
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
  font-family: 'Lora', serif;
  margin: 1rem 0;
  position: relative;
  cursor: pointer;

  h2 {
    position: relative;
    margin: 0;
    padding: 0;
    font-family: 'Avenir', sans-serif;
    font-size: 1.25rem;
    z-index: 1001;
  }

  p {
    margin: 0.5rem 0;

    &:first-child {
      margin-top: 0;
    }

    &:last-child {
      margin-bottom: 0;
    }
  }

  .chapterText {
    text-align: justify;
  }

  img {
    display: inline-block;
    max-width: 100%;
  }

  .centeredImage {
    text-align: center;

    img {
      max-width: 80%;
    }
  }

  .epigraphText {
    width: 60%;
    margin-left: auto;
    margin-right: auto;
    text-align: justify;
  }

  .epigraphCitation {
    width: 60%;
    margin-left: auto;
    margin-right: auto;
    text-align: right;
  }

  .italic {
    font-style: italic;
  }

  .bold {
    font-weight: bold;
  }

  .reset {
    font-style: normal;
    font-weight: normal;
  }
}
</style>
