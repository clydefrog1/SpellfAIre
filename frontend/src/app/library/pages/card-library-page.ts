import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { Router } from '@angular/router';

import { GameCard } from '../../game/components/game-card';
import {
  CardResponse,
  FACTIONS,
  Faction,
  MagicSchool,
  SCHOOLS,
} from '../../game/models/game.models';
import { CardCatalogService } from '../services/card-catalog.service';

type LibraryGroup = {
  id: string;
  name: string;
  color: string | null;
  cards: readonly CardResponse[];
};

type CardTypeFilter = 'BOTH' | 'CREATURE' | 'SPELL';
type CreatureFactionFilter = 'ALL' | Faction | 'NEUTRAL';
type SpellSchoolFilter = 'ALL' | MagicSchool | 'RAW';

@Component({
  selector: 'app-card-library-page',
  standalone: true,
  imports: [CommonModule, GameCard],
  templateUrl: './card-library-page.html',
  styleUrl: './card-library-page.scss',
})
export class CardLibraryPage implements OnInit {
  private readonly router = inject(Router);
  private readonly catalog = inject(CardCatalogService);

  readonly cards = this.catalog.cards;
  readonly loading = this.catalog.loading;
  readonly error = this.catalog.error;

  readonly factions = FACTIONS;
  readonly schools = SCHOOLS;

  readonly searchQuery = signal('');
  readonly cardTypeFilter = signal<CardTypeFilter>('BOTH');
  readonly creatureFactionFilter = signal<CreatureFactionFilter>('ALL');
  readonly spellSchoolFilter = signal<SpellSchoolFilter>('ALL');

  readonly showCreatures = computed(() => this.cardTypeFilter() !== 'SPELL');
  readonly showSpells = computed(() => this.cardTypeFilter() !== 'CREATURE');

  readonly filteredCreatures = computed<readonly CardResponse[]>(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const factionFilter = this.creatureFactionFilter();

    return this.cards()
      .filter(c => c.cardType === 'CREATURE')
      .filter(c => (query ? c.name.toLowerCase().includes(query) : true))
      .filter(c => {
        if (factionFilter === 'ALL') return true;
        if (factionFilter === 'NEUTRAL') return c.faction === null;
        return c.faction === factionFilter;
      })
      .slice()
      .sort(this.byCostThenName);
  });

  readonly filteredSpells = computed<readonly CardResponse[]>(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const schoolFilter = this.spellSchoolFilter();

    return this.cards()
      .filter(c => c.cardType === 'SPELL')
      .filter(c => (query ? c.name.toLowerCase().includes(query) : true))
      .filter(c => {
        if (schoolFilter === 'ALL') return true;
        if (schoolFilter === 'RAW') return c.school === null;
        return c.school === schoolFilter;
      })
      .slice()
      .sort(this.byCostThenName);
  });

  readonly creatureGroups = computed<readonly LibraryGroup[]>(() => {
    const creatures = this.filteredCreatures();
    const groups: LibraryGroup[] = this.factions.map(f => ({
      id: f.id,
      name: f.name,
      color: f.color,
      cards: creatures.filter(c => c.faction === f.id),
    }));

    groups.push({
      id: 'NEUTRAL',
      name: 'Neutral',
      color: null,
      cards: creatures.filter(c => c.faction === null),
    });

    const selected = this.creatureFactionFilter();
    if (selected === 'ALL') return groups;
    return groups.filter(g => g.id === selected);
  });

  readonly spellGroups = computed<readonly LibraryGroup[]>(() => {
    const spells = this.filteredSpells();
    const groups: LibraryGroup[] = this.schools.map(s => ({
      id: s.id,
      name: s.name,
      color: s.color,
      cards: spells.filter(c => c.school === s.id),
    }));

    groups.push({
      id: 'RAW',
      name: 'Raw',
      color: null,
      cards: spells.filter(c => c.school === null),
    });

    const selected = this.spellSchoolFilter();
    if (selected === 'ALL') return groups;
    return groups.filter(g => g.id === selected);
  });

  readonly filteredCount = computed(() => {
    let count = 0;
    if (this.showCreatures()) {
      count += this.creatureGroups().reduce((sum, group) => sum + group.cards.length, 0);
    }
    if (this.showSpells()) {
      count += this.spellGroups().reduce((sum, group) => sum + group.cards.length, 0);
    }
    return count;
  });

  readonly hasMatches = computed(() => this.filteredCount() > 0);

  async ngOnInit(): Promise<void> {
    await this.catalog.loadAll();
  }

  goBack(): void {
    this.router.navigateByUrl('/');
  }

  setSearchQuery(value: string): void {
    this.searchQuery.set(value);
  }

  setCardTypeFilter(value: string): void {
    if (value === 'BOTH' || value === 'CREATURE' || value === 'SPELL') {
      this.cardTypeFilter.set(value);
    }
  }

  setCreatureFactionFilter(value: string): void {
    if (value === 'ALL' || value === 'NEUTRAL' || this.factions.some(f => f.id === value)) {
      this.creatureFactionFilter.set(value as CreatureFactionFilter);
    }
  }

  setSpellSchoolFilter(value: string): void {
    if (value === 'ALL' || value === 'RAW' || this.schools.some(s => s.id === value)) {
      this.spellSchoolFilter.set(value as SpellSchoolFilter);
    }
  }

  trackByCardId(_: number, c: CardResponse): string {
    return c.id;
  }

  private byCostThenName(a: CardResponse, b: CardResponse): number {
    return a.cost - b.cost || a.name.localeCompare(b.name);
  }
}
