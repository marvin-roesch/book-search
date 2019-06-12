<template>
<div class="book-wizard">
  <transition name="fade">
    <router-view></router-view>
  </transition>
</div>
</template>

<script>
export default {
  name: 'BookWizard',
};
</script>

<style lang="scss">
.book-wizard {
  position: relative;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.button-bar {
  margin-top: 1rem;
  display: flex;

  *:last-child {
     margin-left: auto;
  }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity .2s, max-height .2s;
  max-height: 80vh;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
  max-height: 0;
}
</style>
