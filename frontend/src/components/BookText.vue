<template>
<div class="book-text" v-html="content" v-if="content" ref="text">
</div>
<div class="book-text" v-else ref="text">
  <slot></slot>
</div>
</template>

<script>
export default {
  name: 'BookText',
  props: {
    content: String,
  },
  data() {
    return {
      normalizationTimeout: null,
      popupTimeout: null,
    };
  },
  mounted() {
    document.addEventListener('selectionchange', this.onSelectionChange);
  },
  destroyed() {
    document.removeEventListener('selectionchange', this.onSelectionChange);
  },
  methods: {
    onSelectionChange() {
      const selection = document.getSelection();
      const startNode = selection.anchorNode;
      const endNode = selection.focusNode;
      const startOffset = selection.anchorOffset;
      const endOffset = selection.focusOffset;

      const affectedBySelection = !selection.isCollapsed
        && startNode !== null && this.$refs.text.contains(startNode)
        && endNode !== null && this.$refs.text.contains(endNode);

      if (!affectedBySelection) {
        this.removeSelectionMarker('selection-start-marker');
        this.removeSelectionMarker('selection-end-marker');
      } else if (startNode !== endNode || startOffset > endOffset) {
        this.insertSelectionMarker('selection-start-marker', startNode, startOffset);
        this.insertSelectionMarker('selection-end-marker', endNode, endOffset);
      } else {
        this.insertSelectionMarker('selection-end-marker', endNode, endOffset);
        this.insertSelectionMarker('selection-start-marker', startNode, startOffset);
      }

      clearTimeout(this.normalizationTimeout);
      clearTimeout(this.popupTimeout);
      this.normalizationTimeout = setTimeout(() => {
        this.$refs.text.normalize();
      }, 100);
      if (affectedBySelection) {
        this.popupTimeout = setTimeout(() => {
          // TODO: Display popup
        }, 500);
      }
    },
    insertSelectionMarker(id, node, offset) {
      const marker = document.getElementById(id) || document.createElement('span');
      marker.id = id;

      if (node.nodeType === Node.TEXT_NODE) {
        node.splitText(offset);
        node.parentNode.insertBefore(marker, node.nextSibling);
      } else if (node.nodeType === Node.ELEMENT_NODE) {
        if (offset === 0 && node.previousElementSibling !== null) {
          node.previousElementSibling.append(marker);
        } else if (offset > 0 && node.nextElementSibling !== null) {
          node.nextElementSibling.prepend(marker);
        }
      }
    },
    removeSelectionMarker(id) {
      const marker = document.getElementById(id);
      if (marker !== null && this.$refs.text.contains(marker)) {
        marker.remove();
      }
    },
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

  .h1 {
    text-align: center;
    font-size: 1.5rem;
  }

  .h2 {
    text-align: center;
    font-size: 1.25rem;
  }

  .h3 {
    text-align: center;
    font-size: 1.1rem;
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

  .result-paragraph {
    background: aliceblue;
  }
}
</style>
