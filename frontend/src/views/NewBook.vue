<template>
<div class="new-book">
  <div class="book-uploader">
    <input id="book-upload-field" type="file" accept="application/epub+zip" required @change="bookSelected"
           :disabled="selectedFileName !== null">
    <svg width="150" class="book-icon">
      <clipPath id="ripple-clip">
        <circle cx="66" cy="66" r="66" class="ripple"></circle>
      </clipPath>
      <circle cx="75" cy="75" r="68" class="upload-progress" fill="none" stroke="#4AC694" stroke-width="14" stroke-dasharray="427.257"
              :stroke-dashoffset="427.257 * (1 - uploadProgress)"></circle>
      <use x="10" y="10" width="130" height="130" xlink:href="../assets/book-open-flat.svg#book" class="book"></use>
      <use x="9" y="9" clip-path="url(#ripple-clip)" width="132" height="132" xlink:href="../assets/book-open-flat.svg#book-color"
           class="colored-book"></use>
    </svg>
    <label for="book-upload-field" v-if="selectedFileName === null">Drop your .epub file here</label>
    <label for="book-upload-field" v-else>Uploading {{ selectedFileName }}...</label>
  </div>
  <transition name="fade">
    <div class="toc" v-if="toc !== null">
      <TableOfContents :entries="toc" @change="updateTableOfContents"></TableOfContents>
    </div>
  </transition>
</div>
</template>

<script>
import axios from 'axios';
import TableOfContents from '@/components/TableOfContents.vue';

export default {
  name: 'new-book',
  components: { TableOfContents },
  methods: {
    async bookSelected(event) {
      const [file] = event.target.files;
      if (!file) {
        this.selectedFileName = null;
        this.uploadProgress = 0;
        return;
      }
      this.selectedFileName = file.name;
      const formData = new FormData();
      formData.append('book', file);
      try {
        const { data: { toc } } = await axios.put(
          '/api/book',
          formData,
          {
            headers: {
              'Content-Type': 'multipart/form-data',
            },
            onUploadProgress: (function (progressEvent) {
              this.uploadProgress = progressEvent.loaded / progressEvent.total;
            }).bind(this),
          },
        );
        this.toc = this.mapTableOfContents(toc);
      } catch (error) {

      }
    },
    mapTableOfContents(toc) {
      return toc.map(entry => ({ ...entry, selected: true, children: this.mapTableOfContents(entry.children) }));
    },
    updateTableOfContents({ id, selected }) {
      this.updateTableOfContentsEntry(this.toc, id.split('/'), selected)
    },
    updateTableOfContentsEntry(toc, path, selected) {
      const [head, ...tail] = path;
      const entry = toc.filter(entry => entry.id === head)[0];
      if (tail.length === 0) {
        entry.selected = selected;
      } else {
        this.updateTableOfContentsEntry(entry.children, tail, selected);
      }
    },
  },
  data() {
    return {
      selectedFileName: null,
      uploadProgress: 0,
      toc: null,
    };
  },
};
</script>

<style lang="scss">
.toc {
  flex-grow: 1;
  overflow-y: scroll;
}

.new-book {
  padding: 2rem;
  background: white;
  border-radius: 3px;
  border: 1px solid rgba(0, 0, 0, 0.05);
  box-shadow: 0 0.75rem 1rem rgba(0, 0, 0, 0.1);
  width: 40vw;
  height: 80vh;
  display: flex;
  flex-direction: column;

  h2 {
    font-weight: 400;
    margin: 0 0 0.5rem;
  }

  &-footer {
    display: flex;
    margin-top: 0.5rem;
    align-items: center;

    * {
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }
  }

  .book-uploader {
    position: relative;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    border: 1px solid rgba(0, 24, 46, 0.1);
    border-radius: 5px;
    padding: 1rem;
    margin-bottom: 1rem;

    .book {
      opacity: 1;
    }

    .ripple {
      transform: scale(0);
      transition: transform 0.25s ease-in-out;
      transform-origin: 50% 50%;
    }

    .colored-book {
      transform-origin: 50% 50%;
    }

    input {
      opacity: 0;
      width: 100%;
      height: 100%;
      position: absolute;
      cursor: pointer;
    }

    input:valid ~ .book-icon, input:disabled ~ .book-icon {
      .ripple {
        transform: scale(1);
      }

      .colored-book {
        animation-name: book-wiggle;
        animation-duration: 0.25s;
        animation-delay: 0.1s;
      }
    }

    label {
      margin-top: 0.5rem;
      color: rgba(0, 0, 0, 0.4);
    }

    .upload-progress {
      transform-origin: 50% 50%;
      transform: rotate(-90deg);
      transition: stroke-dashoffset 0.25s linear;
    }
  }
}

@keyframes book-wiggle {
  0% {
    transform: rotate(0);
  }
  25% {
    transform: rotate(6deg);
  }
  75% {
    transform: rotate(-6deg);
  }
  100% {
    transform: rotate(0);
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity .2s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
}
</style>
