import { TestBed } from '@angular/core/testing';

import { BattlefieldCreature } from './battlefield-creature';
import { BoardCreatureResponse, CardResponse } from '../models/game.models';

describe('BattlefieldCreature badge precedence', () => {
  const testCard: CardResponse = {
    id: 'card-1',
    name: 'Test Creature',
    cardType: 'CREATURE',
    cost: 2,
    attack: 2,
    health: 3,
    faction: 'KINGDOM',
    keywords: [],
    school: null,
    rulesText: null,
    flavorText: null,
  };

  const makeCreature = (overrides: Partial<BoardCreatureResponse> = {}): BoardCreatureResponse => ({
    instanceId: 'creature-1',
    cardId: 'card-1',
    attack: 2,
    health: 3,
    maxHealth: 3,
    canAttack: false,
    hasAttackedThisTurn: false,
    frozenBlocksAttacksThisTurn: false,
    keywords: [],
    statuses: [],
    position: 0,
    ...overrides,
  });

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [BattlefieldCreature],
    }).compileComponents();
  });

  it('shows flashing frozen badge for player creature blocked by Frozen this turn', () => {
    const fixture = TestBed.createComponent(BattlefieldCreature);
    fixture.componentRef.setInput('side', 'player');
    fixture.componentRef.setInput('card', testCard);
    fixture.componentRef.setInput('creature', makeCreature({
      frozenBlocksAttacksThisTurn: true,
      canAttack: false,
      hasAttackedThisTurn: false,
      statuses: [],
    }));

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.frozen-blocked-badge')).toBeTruthy();
    expect(element.querySelector('.frozen-badge')).toBeFalsy();
    expect(element.querySelector('.attack-ready-badge')).toBeFalsy();
  });

  it('shows static frozen badge for enemy creature with Frozen status', () => {
    const fixture = TestBed.createComponent(BattlefieldCreature);
    fixture.componentRef.setInput('side', 'opponent');
    fixture.componentRef.setInput('card', testCard);
    fixture.componentRef.setInput('creature', makeCreature({
      statuses: ['FROZEN'],
      frozenBlocksAttacksThisTurn: false,
    }));

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.frozen-badge')).toBeTruthy();
    expect(element.querySelector('.frozen-blocked-badge')).toBeFalsy();
    expect(element.querySelector('.attack-ready-badge')).toBeFalsy();
  });

  it('shows sword badge for attack-ready player creature when no frozen badge applies', () => {
    const fixture = TestBed.createComponent(BattlefieldCreature);
    fixture.componentRef.setInput('side', 'player');
    fixture.componentRef.setInput('card', testCard);
    fixture.componentRef.setInput('creature', makeCreature({
      canAttack: true,
      hasAttackedThisTurn: false,
      statuses: [],
      frozenBlocksAttacksThisTurn: false,
    }));

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.attack-ready-badge')).toBeTruthy();
    expect(element.querySelector('.frozen-blocked-badge')).toBeFalsy();
    expect(element.querySelector('.frozen-badge')).toBeFalsy();
  });

  it('keeps sword badge for player creature with Frozen status if not blocked this turn', () => {
    const fixture = TestBed.createComponent(BattlefieldCreature);
    fixture.componentRef.setInput('side', 'player');
    fixture.componentRef.setInput('card', testCard);
    fixture.componentRef.setInput('creature', makeCreature({
      canAttack: true,
      hasAttackedThisTurn: false,
      statuses: ['FROZEN'],
      frozenBlocksAttacksThisTurn: false,
    }));

    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('.attack-ready-badge')).toBeTruthy();
    expect(element.querySelector('.frozen-blocked-badge')).toBeFalsy();
  });
});
