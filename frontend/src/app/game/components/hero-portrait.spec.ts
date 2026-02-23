import { TestBed } from '@angular/core/testing';

import { HeroPortrait } from './hero-portrait';

describe('HeroPortrait loadout display', () => {
  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [HeroPortrait],
    }).compileComponents();
  });

  it('renders faction and school loadout chips', () => {
    const fixture = TestBed.createComponent(HeroPortrait);

    fixture.componentRef.setInput('faction', 'NECROPOLIS');
    fixture.componentRef.setInput('magicSchool', 'SHADOW');
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    const loadoutText = element.querySelector('.hero-loadout-row')?.textContent ?? '';

    expect(loadoutText).toContain('Necropolis');
    expect(loadoutText).toContain('Shadow');
  });
});
