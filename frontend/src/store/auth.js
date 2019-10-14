import axios from 'axios';

const initialState = {
  identity: null,
};

const getters = {
  authorized(state) {
    return state.identity !== null;
  },
  hasPermission(state) {
    return permission => state.identity.permissions.includes(permission);
  },
};

const actions = {
  async checkIdentity({ commit }) {
    try {
      const { data: identity } = await axios.get('/api/auth/identity');
      commit('setIdentity', identity);
      return true;
    } catch (error) {
      if (error.response && error.response.status === 401) {
        commit('setIdentity', null);
      } else {
        throw error;
      }
    }
    return false;
  },
  async login({ commit, dispatch }, payload) {
    try {
      const { data: { message, firstLogin, user: identity } } = await axios.post(
        '/api/auth/login',
        payload,
      );
      commit('setIdentity', identity);
      dispatch('notifications/push', { type: 'success', message }, { root: true });
      return firstLogin;
    } catch (error) {
      throw error;
    }
  },
  async logout({ commit, dispatch }) {
    const { data: { message } } = await axios.post('/api/auth/logout');
    commit('setIdentity', null);
    dispatch('notifications/push', { type: 'success', message }, { root: true });
  },
};

const mutations = {
  setIdentity(state, identity) {
    state.identity = identity;
  },

  resetIdentity(state) {
    state.identity = null;
  },
};

export default {
  namespaced: true,
  state: initialState,
  getters,
  actions,
  mutations,
};
