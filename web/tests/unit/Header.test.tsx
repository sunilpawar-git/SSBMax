import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { Header } from '../../src/components/common/Header';
import { strings } from '../../src/constants/strings';

describe('Header component', () => {
  beforeEach(() => {
    localStorage.setItem('theme', 'dark');
    document.documentElement.classList.add('dark');
  });

  afterEach(() => {
    localStorage.clear();
    document.documentElement.classList.remove('dark');
  });

  it('renders header title and tagline from string resources', () => {
    render(<Header />);
    expect(screen.getByText(strings.header.title)).toBeInTheDocument();
    expect(screen.getByText(strings.header.tagline)).toBeInTheDocument();
  });

  it('renders online status badge', () => {
    render(<Header />);
    const badge = screen.getByTestId('online-status-badge');
    expect(badge).toBeInTheDocument();
  });

  it('toggles theme when theme toggle button is clicked', () => {
    render(<Header />);
    const toggleButton = screen.getByTestId('theme-toggle-button');
    expect(toggleButton).toBeInTheDocument();
    expect(document.documentElement.classList.contains('dark')).toBe(true);

    fireEvent.click(toggleButton);
    expect(document.documentElement.classList.contains('dark')).toBe(false);
  });
});
