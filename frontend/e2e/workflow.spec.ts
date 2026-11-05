import { expect, test } from '@playwright/test';

test('create, approve, execute, and inspect a simulated workflow', async ({ page }) => {
  const title = `Browser workflow ${Date.now()}`;
  await page.goto('/');
  await expect(page.getByRole('heading', { name: 'Create a work item' })).toBeVisible();
  await page.getByLabel('Title').fill(title);
  await page.getByLabel('Description').fill('Return a documented application health status.');
  await page.getByLabel('Acceptance criteria').fill('HTTP 200 and automated test.');
  await page.getByRole('button', { name: 'Create work item' }).click();

  const item = page.locator('.work-item').filter({ hasText: title });
  await expect(item).toBeVisible();
  await item.getByRole('button', { name: 'Create plan' }).click();
  await item.getByRole('button', { name: 'Approve plan' }).click();
  await item.getByRole('button', { name: 'Execute mock' }).click();

  await expect(item.getByText('SIMULATED', { exact: true })).toBeVisible();
  await expect(item.getByText('Quality gate:', { exact: false })).toContainText('PASSED');
  await expect(item.getByRole('heading', { name: 'Audit trail' })).toBeVisible();
  await expect(item.getByText('QUALITY GATE PASSED')).toBeVisible();
});
