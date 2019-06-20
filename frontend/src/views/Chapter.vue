<template>
<div class="chapter">
  <div class="chapter-content" v-html="content"></div>
</div>
</template>

<script>
export default {
  name: 'chapter',
  data() {
    return {
      title: '',
      content: '',
    };
  },
  async mounted() {
    const { id } = this.$route.params;
    try {
      const { data: { title, content } } = await this.$api.get(`/books/chapter/${id}`);

      this.title = title;
      this.content = content;
    } catch (error) {
      this.$handleApiError(error);
    }
  },
};
</script>

<style lang="scss">
.chapter {
  box-sizing: border-box;
  background: white;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
  padding: 4rem;
  font-family: 'Lora', serif;
  margin: 1rem 0;
  position: relative;
  width: 50%;
  max-width: 960px;

  @media (max-width: 960px) {
    width: 100%;
  }

  h2 {
    position: relative;
    margin: 0;
    padding: 0;
    font-family: 'Avenir', sans-serif;
    font-size: 1.25rem;
    z-index: 1001;
    text-align: center;
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
    text-indent: 1rem;
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
