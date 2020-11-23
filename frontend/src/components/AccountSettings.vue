<template>
<Card class="account-settings" title="Account Settings">
  <div class="account-settings-section">
    <h3>Appearance</h3>
    <CheckBox
      name="dark-mode"
      :value="darkMode"
      @input="$store.commit('setDarkMode', $event.target.checked)"
    >
      Enable Dark Mode (Currently only affects this device)
    </CheckBox>
  </div>
  <form class="account-settings-section">
    <h3>Search behavior</h3>
    <div class="account-settings-scope-container">
      <span class="account-settings-scope-label">Search in</span>
      <div class="account-settings-scope">
        <input
          type="radio"
          id="account-settings-paragraphs-scope"
          value="paragraphs"
          v-model="defaultSearchScope">
        <label for="account-settings-paragraphs-scope">paragraphs</label>
      </div>
      <div class="account-settings-scope">
        <input
          type="radio"
          id="account-settings-chapters-scope"
          value="chapters"
          v-model="defaultSearchScope">
        <label for="account-settings-chapters-scope">chapters</label>
      </div>
      <span>by default</span>
    </div>
    <CheckBox
      name="group-results-by-default"
      :value="groupResultsByDefault"
      @input="groupResultsByDefault = $event.target.checked"
    >
      Group search results by default
    </CheckBox>
    <div class="card-footer">
      <Button slim @click="changeSearchSettings" :loading="verifyingSearchSettings">Save</Button>
    </div>
  </form>
  <form class="account-settings-section">
    <h3>Change password</h3>
    <TextField
      type="password"
      name="old-password"
      v-model="oldPassword"
      placeholder="Current password"
    >
      <template slot="icon">
      <LockIcon></LockIcon>
      </template>
    </TextField>
    <TextField
      type="password"
      name="new-password"
      v-model="newPassword"
      placeholder="New password"
    >
      <template slot="icon">
      <KeyIcon></KeyIcon>
      </template>
    </TextField>
    <TextField
      type="password"
      name="repeat-password"
      v-model="newPasswordRepeat"
      placeholder="Repeat password"
    >
      <template slot="icon">
      <RepeatIcon></RepeatIcon>
      </template>
    </TextField>
    <div class="card-footer">
      <a href="#" @click.prevent="$router.back()">Cancel</a>
      <Button slim @click="changePassword" :loading="verifyingPassword">Change</Button>
    </div>
  </form>
</Card>
</template>

<script>
import { KeyIcon, LockIcon, RepeatIcon } from 'vue-feather-icons';
import { mapState } from 'vuex';
import TextField from '@/components/TextField.vue';
import Button from '@/components/Button.vue';
import Card from '@/components/Card.vue';
import CheckBox from '@/components/CheckBox.vue';

export default {
  name: 'AccountSettings',
  components: {
    CheckBox,
    Card,
    Button,
    TextField,
    LockIcon,
    KeyIcon,
    RepeatIcon,
  },
  data() {
    return {
      defaultSearchScope: this.$store.state.auth.identity.defaultSearchScope,
      groupResultsByDefault: this.$store.state.auth.identity.groupResultsByDefault,
      oldPassword: '',
      newPassword: '',
      newPasswordRepeat: '',
      verifyingSearchSettings: false,
      verifyingPassword: false,
    };
  },
  computed: mapState(['darkMode']),
  methods: {
    async changeSearchSettings() {
      const {
        defaultSearchScope, groupResultsByDefault,
      } = this;
      this.verifyingSearchSettings = true;
      try {
        const { data: { message, identity } } = await this.$api.patch(
          '/auth/search-settings',
          { defaultScope: defaultSearchScope, groupByDefault: groupResultsByDefault },
        );
        this.$store.commit('auth/setIdentity', identity);
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
      this.verifyingSearchSettings = false;
    },
    async changePassword() {
      const {
        oldPassword, newPassword, newPasswordRepeat,
      } = this;
      this.verifyingPassword = true;
      try {
        const { data: { message } } = await this.$api.patch(
          '/auth/password',
          { oldPassword, newPassword, newPasswordRepeat },
        );
        this.$notifications.success(message);
      } catch (error) {
        this.$handleApiError(error);
      }
      this.verifyingPassword = false;
    },
  },
};
</script>

<style lang="scss">
.account-settings {
  margin: 0 auto;
  width: 50vw;
  max-width: $max-content-width;
  box-sizing: border-box;

  form {
    display: flex;
    flex-direction: column;
    align-items: stretch;

    .checkbox {
      display: inline-block;
      margin-top: 0.5rem;
      margin-right: 0.5rem;

      &:last-child {
        margin-right: 0;
      }
    }
  }

  &-section {
    h3 {
      padding: 0;
      margin: 0;
    }

    margin-bottom: 0.5rem;

    &:last-child {
      margin-bottom: 0;
    }
  }

  &-scope {
    display: flex;
    align-items: center;
    margin-left: 0.25rem;

    &-container {
      display: flex;
      align-items: stretch;
      flex-direction: row;
    }

    input {
      margin: 0 0.125rem 0 0;
    }

    label:hover {
      cursor: pointer;
    }

    &:last-of-type {
      margin-right: 0.25rem;
    }
  }

  @media (max-width: 640px) {
    width: 100%;
  }
}
</style>
