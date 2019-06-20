<template>
<div class="progress-spinner-container">
  <div class="progress-spinner"></div>
</div>
</template>

<style scoped lang="scss">
.progress-spinner {
  color: $primary;
  position: relative;
  text-indent: -9999em;
  animation-delay: -0.16s;
  transform: translateY(-1.25rem);

  &-container {
    display: flex;
    justify-content: center;
  }

  &, &:before, &:after {
    border-radius: 50%;
    width: 1.25rem;
    height: 1.25rem;
    animation-fill-mode: both;
    animation: dot-loader 1.8s infinite ease-in-out;
  }

  &:before, &:after {
    content: '';
    position: absolute;
    top: 0;
  }

  &:before {
    left: -1.75rem;
    animation-delay: -0.32s;
  }

  &:after {
    left: 1.75rem;
  }
}

@keyframes dot-loader {
  0%,
  80%,
  100% {
    box-shadow: 0 1.25rem 0 -0.65rem;
  }
  40% {
    box-shadow: 0 1.25rem 0 0;
  }
}
</style>
