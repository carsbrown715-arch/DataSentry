import cleaningService, { type CleaningOptionMetaView } from './cleaning';

const CACHE_TTL = 60 * 1000;

class CleaningMetaService {
  private cache?: { time: number; data: CleaningOptionMetaView };

  async getOptions(forceRefresh = false): Promise<CleaningOptionMetaView | null> {
    const now = Date.now();
    if (!forceRefresh && this.cache && now - this.cache.time < CACHE_TTL) {
      return this.cache.data;
    }
    try {
      const data = await cleaningService.getOptionMeta();
      if (!data) {
        return null;
      }
      this.cache = { time: now, data };
      return data;
    } catch (error) {
      return null;
    }
  }
}

export default new CleaningMetaService();
