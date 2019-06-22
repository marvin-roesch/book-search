<template>
<div class="book-text" v-html="content">
</div>
</template>

<script>
export default {
  name: 'BookText',
  props: {
    content: String,
  },
};
</script>

<style lang="scss">
.book-text {
  font-family: 'Lora', serif;

  h2 {
    position: relative;
    margin: 0;
    padding: 0;
    font-family: 'Nunito Sans', sans-serif;
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

  * + .embed {
    margin-top: 1rem;
  }

  .embed {
    font-family: 'Nunito Sans', sans-serif;
    box-sizing: border-box;
    display: block;
    padding: 0 1.5rem;

    & + * {
      margin-top: 1rem;
    }

    & + .embed {
      margin-top: 0;
    }
  }

  samp {
    font-family: 'Nunito Sans', sans-serif;
    text-align: justify;
    display: block;
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
