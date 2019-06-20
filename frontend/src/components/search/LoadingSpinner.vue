<template>
<div class="loading-spinner-container">
  <div class="loading-spinner"></div>
</div>
</template>

<style scoped lang="scss">
.loading-spinner {
  color: $primary;
  font-size: 10px;
  position: relative;
  text-indent: -9999em;
  animation-delay: -0.16s;
  transform: translateY(-2.5em);

  &, &:before, &:after {
    border-radius: 50%;
    width: 2.5em;
    height: 2.5em;
    animation-fill-mode: both;
    animation: dot-loader 1.8s infinite ease-in-out;
  }

  &:before, &:after {
    content: '';
    position: absolute;
    top: 0;
  }

  &:before {
    left: -3.5em;
    animation-delay: -0.32s;
  }

  &:after {
    left: 3.5em;
  }

  &-container {
    display: flex;
    justify-content: center;
  }
}

@keyframes dot-loader {
  0%,
  80%,
  100% {
    box-shadow: 0 2.5em 0 -1.3em;
  }
  40% {
    box-shadow: 0 2.5em 0 0;
  }
}
</style>
