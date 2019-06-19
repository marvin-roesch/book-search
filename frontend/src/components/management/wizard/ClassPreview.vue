<template>
<div class="class-preview">
  <pre v-highlightjs="styleCode" class="class-preview-source"><code class="css"></code></pre>
  <div v-shadow class="class-preview-sample" v-html="cls.sample" ref="preview">
  </div>
</div>
</template>

<script>
import cssBeautify from 'cssbeautify';

export default {
  name: 'ClassPreview',
  props: {
    cls: Object,
  },
  mounted() {
    const style = document.createElement('style');
    style.type = 'text/css';
    style.appendChild(document.createTextNode('@import url(\'/book-preview.css\');'));
    style.appendChild(document.createTextNode(this.cls.styles));
    this.$refs.preview.shadowRoot.prepend(style);
    this.styleCode = this.getStyle(style.sheet);
  },
  data() {
    return {
      styleCode: '',
    };
  },
  methods: {
    getStyle(stylesheet) {
      const rules = stylesheet.rules || stylesheet.cssRules;
      if (!rules) {
        return '';
      }
      let buffer = '';
      for (let i = 0; i < rules.length; i++) {
        const rule = rules[i];
        const classRegex = new RegExp(`\\.${this.cls.name}([\\s,]|$)`);
        if (!rule.selectorText || !rule.selectorText.match(classRegex)) {
          continue;
        }
        let ruleBuffer = '';
        if (rule.cssText) {
          ruleBuffer += rule.cssText;
        } else {
          ruleBuffer += rule.style.cssText;
        }

        if (ruleBuffer.indexOf(rule.selectorText) === -1) {
          ruleBuffer = `${rule.selectorText} {${ruleBuffer}}`;
        }

        buffer += ruleBuffer;
      }
      return cssBeautify(buffer);
    },
  },
};
</script>

<style lang="scss">
.class-preview {
  padding-bottom: 1rem;

  &-source {
    margin-top: 0;
  }

  &-sample {
    overflow: auto;
    box-sizing: border-box;
    padding: 0 1rem;
    border-left: 5px solid rgba(0, 0, 0, 0.1);
  }
}
</style>
