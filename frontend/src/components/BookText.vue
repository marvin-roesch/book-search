<template>
<div class="book-text" ref="text">
  <slot>
    <div v-html="content"></div>
  </slot>
  <transition name="fade">
    <div class="book-text-popup" v-show="displayPopup" ref="popup">
      Quote:
      <a href="#" @click.prevent.stop="copyPlainQuote">Plain</a> &middot;
      <a href="#" @click.prevent.stop="copyDiscordQuote">Discord</a> &middot;
      <a href="#" @click.prevent.stop="copyWikiQuote">Wiki</a>
    </div>
  </transition>
</div>
</template>

<script>
export default {
  name: 'BookText',
  props: {
    bookTitle: String,
    chapterTitle: String,
    content: String,
  },
  data() {
    return {
      normalizationTimeout: null,
      displayPopup: false,
    };
  },
  mounted() {
    document.addEventListener('selectionchange', this.onSelectionChange);
    document.addEventListener('mouseup', this.showPopup);
    document.addEventListener('touchstart', this.showPopup);
  },
  destroyed() {
    document.removeEventListener('selectionchange', this.onSelectionChange);
    document.removeEventListener('mouseup', this.showPopup);
    document.removeEventListener('touchstart', this.showPopup);
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
        this.hidePopup();
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
      this.normalizationTimeout = setTimeout(() => {
        if (this.$refs.text) {
          this.$refs.text.normalize();
        }
      }, 100);
    },
    insertSelectionMarker(id, node, offset) {
      const marker = document.getElementById(id) || document.createElement('span');

      if (marker === node) {
        return;
      }

      marker.id = id;

      if (node.nodeType === Node.TEXT_NODE) {
        node.splitText(offset);
        node.parentNode.insertBefore(marker, node.nextSibling);
      } else if (node.nodeType === Node.ELEMENT_NODE) {
        if (offset === 0) {
          node.prepend(marker);
        } else if (offset > 0) {
          node.append(marker);
        }
      }
    },
    removeSelectionMarker(id) {
      const marker = document.getElementById(id);
      if (marker !== null && this.$refs.text.contains(marker)) {
        marker.remove();
      }
    },
    hidePopup() {
      this.displayPopup = false;
    },
    showPopup(event) {
      const { popup } = this.$refs;
      const startMarker = document.getElementById('selection-start-marker');
      const endMarker = document.getElementById('selection-end-marker');

      if (startMarker === null || !this.$refs.text.contains(startMarker)) {
        return;
      }

      let x = startMarker.offsetLeft;
      let y = Math.min(startMarker.offsetTop, endMarker.offsetTop);
      if (y < startMarker.offsetTop) {
        x = endMarker.offsetLeft;
      }

      if (event instanceof TouchEvent) {
        y += 50;
      }

      popup.style.left = `${x}px`;
      popup.style.top = `${y}px`;
      this.displayPopup = true;
    },
    copyPlainQuote() {
      this.copySelection('', '');
    },
    copyDiscordQuote() {
      this.copySelection(
        '',
        `\n> \n> *${this.bookTitle} - ${this.chapterTitle}*`,
        s => s.split('\n').map(l => `> ${l}`).join('\n'),
      );
    },
    copyWikiQuote() {
      this.copySelection(
        '{{quote\n| ',
        `\n| ${this.bookTitle} - ${this.chapterTitle}\n}}`,
      );
    },
    copySelection(prefix, suffix, transform = s => s) {
      const el = document.createElement('textarea');
      const selection = this.normalizeText(document.getSelection().toString().trim());
      el.value = `${prefix}${transform(selection)}${suffix}`;
      document.body.appendChild(el);
      el.select();

      document.execCommand('copy');

      document.body.removeChild(el);

      this.$notifications.success('The quote has been successfully copied to your clipboard!');
    },
    normalizeText(text) {
      return text
        .replace(/[\u00AB\u00BB\u201C\u201D\u201E]/g, '"') // Double quotes
        .replace(/[\u0091\u0092\u2018\u2019\u201A\u2039\u203A\uFF07]/, '\''); // Single quotes
    },
  },
};
</script>

<style lang="scss">
.book-text {
  font-family: 'Lora', serif;
  position: relative;
  word-break: break-word;

  &-popup {
    position: absolute;
    padding: 0.25rem;
    background: var(--section-bg);
    border: 1px solid rgba(0, 0, 0, 0.1);
    border-radius: 3px;
    box-shadow: 0 0.25rem 1rem rgba(0, 0, 0, 0.1);
    transform: translateY(-100%);
    margin-top: -0.5rem;
    font-family: 'Nunito Sans', sans-serif;
    z-index: 2000;
    max-width: 1000px;
    user-select: none;

    &:after {
      position: absolute;
      box-sizing: border-box;
      background: var(--section-bg);
      content: '';
      width: 0.5rem;
      height: 0.5rem;
      top: auto;
      bottom: 0;
      left: 0.7rem;
      margin-bottom: -0.25rem;
      transform-origin: 50%;
      transform: rotate(45deg);
      border: 1px solid rgba(0, 0, 0, 0.1);
      border-top: none;
      border-left: none;
    }

    @media (max-width: $max-content-width) {
      position: fixed;
      left: 0 !important;
      right: 0 !important;
      top: auto !important;
      bottom: 0 !important;
      margin-top: 0;
      padding: 1rem;
      transform: none;
      border-radius: 0;
      border-left: none;
      border-right: none;
      border-bottom: none;

      &:after {
        display: none;
      }
    }
  }

  h2 {
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

  h1, .h1 {
    text-align: center;
    font-size: 1.5rem;
  }

  h2, .h2 {
    text-align: center;
    font-size: 1.25rem;
  }

  h3, .h3 {
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
    margin: 0 auto;
  }

  .centeredImage, .fullWidthImage, .iconImage {
    text-align: center;
  }

  .centeredImage img {
    max-width: 80%;
  }

  .fullWidthImage img {
    max-width: 100%;
  }

  .iconImage img {
    max-width: 20%;
  }

  p.epigraphText {
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

  .footnote {
    font-size: 0.9rem;
    padding: 0.5rem 0;
    color: var(--muted-text-color);
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

  .graphicNovelLine, .graphicNovelThought, .graphicNovelSfx {
    .graphicNovelSpeaker {
      display: block;
      text-align: left;
      text-transform: uppercase;
      font-style: normal;
      font-weight: bold;
    }

    .graphicNovelText {
      display: block;
      box-sizing: border-box;
      padding-left: 2rem;

      &:before, &:after {
        content: none;
      }
    }
  }

  .graphicNovelThought {
    .graphicNovelSpeaker:after {
      content: ' (thinking)';
      font-weight: normal;
      text-transform: none;
      user-select: auto;
      font-size: 0.8rem;
    }

    .graphicNovelText {
      font-style: italic;
    }
  }

  hr, .graphicNovelPanelSplit {
    display: block;
    overflow: hidden;
    width: 50%;
    height: 0;
    margin: 1rem auto;
    border: 1px solid rgba(0, 0, 0, 0.2);
  }

  .reset {
    font-style: normal;
    font-weight: normal;
  }

  .result-paragraph {
    background: var(--chapter-highlight-color);
  }
}
</style>
