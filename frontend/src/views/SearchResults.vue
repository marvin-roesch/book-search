<template>
<div class="search-results">
  <div :class="{ 'search-result': true, 'search-result-with-siblings': result.showSiblings }"
       @click="result.showSiblings = !result.showSiblings"
       v-for="(result, index) in results" :key="index">
    <h2>{{ result.book }} - {{ result.chapter }}</h2>
    <BidirectionalExpandable :expanded="result.showSiblings" :visible-height="24">
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
      </template>
    </BidirectionalExpandable>
  </div>
</div>
</template>

<script>
import axios from 'axios';
import BidirectionalExpandable from '@/components/BidirectionalExpandable.vue';

export default {
  name: 'search-results',
  components: { BidirectionalExpandable },
  mounted() {
    this.search();
  },
  data() {
    return {
      results: [],
    };
  },
  methods: {
    async search() {
      const { data } = await axios.post('/api/search', {
        query: this.$route.query.q,
      });
      this.results = data.map(({ book, chapter, paragraphs }) => {
        const mainParagraph = paragraphs.find(p => p.main);
        return {
          book,
          chapter,
          mainParagraph,
          prevParagraphs: paragraphs.filter(p => p.position < mainParagraph.position),
          nextParagraphs: paragraphs.filter(p => p.position > mainParagraph.position),
          showSiblings: false,
        };
      });
    },
  },
};
</script>

<style lang="scss">
.search-results {
  padding: 1rem;
  width: 50vw;

  .search-result {
    background: white;
    border-radius: 3px;
    border: 1px solid rgba(0, 0, 0, 0.05);
    box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
    padding: 1rem;
    font-family: 'Lora', serif;
    margin: 2rem 0;
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
}
</style>
