<template>
<div class="book-uploader">
  <input id="book-upload-field" type="file" accept="application/epub+zip" required @change="fileSelected"
         :disabled="selectedFileName !== null">
  <svg width="150" class="book-icon">
    <clipPath id="ripple-clip">
      <circle cx="66" cy="66" r="66" class="ripple"></circle>
    </clipPath>
    <circle cx="75" cy="75" r="68" class="upload-progress" fill="none" stroke="#4AC694" stroke-width="14" stroke-dasharray="427.257"
            :stroke-dashoffset="427.257 * (1 - uploadProgress)"></circle>
    <use x="10" y="10" width="130" height="130" xlink:href="@/assets/book-open-flat.svg#book" class="book"></use>
    <use x="9" y="9" clip-path="url(#ripple-clip)" width="132" height="132" xlink:href="@/assets/book-open-flat.svg#book-color"
         class="colored-book"></use>
  </svg>
  <label for="book-upload-field" v-if="selectedFileName === null">Drop your .epub file here</label>
  <label for="book-upload-field" v-else>Uploading {{ selectedFileName }}...</label>
</div>
</template>

<script>
export default {
  name: 'BookUploader',
  data() {
    return {
      selectedFileName: null,
      uploadProgress: 0,
    };
  },
  methods: {
    async fileSelected(event) {
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
        const { data: { id } } = await this.$api.put(
          '/book',
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
        this.$router.push({ name: 'book-metadata', params: { id } });
      } catch (error) {

      }
    },
  },
};
</script>

<style scoped lang="scss">
.book-uploader {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;

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
</style>
